package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class TownSystem extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private final Map<String, TownBuilding> buildings = new HashMap<>();
    private final Map<Location, BuildingData> placedBuildings = new HashMap<>();
    private final Map<UUID, Location> activeSessions = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        loadBuildings();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Плагин успешно запущен! Загружено зданий: " + buildings.size());
    }

    private void loadBuildings() {
        ConfigurationSection buildingsSection = config.getConfigurationSection("buildings");
        if (buildingsSection == null) {
            getLogger().severe("❌ Не найдена секция 'buildings' в config.yml!");
            return;
        }

        for (String buildingId : buildingsSection.getKeys(false)) {
            try {
                ConfigurationSection bConfig = buildingsSection.getConfigurationSection(buildingId);
                if (bConfig == null) {
                    getLogger().warning("⚠️ Нет данных для здания " + buildingId);
                    continue;
                }

                TownBuilding building = new TownBuilding(
                        buildingId,
                        bConfig.getString("display-name", buildingId),
                        bConfig.getInt("max-level", 1),
                        bConfig.getStringList("unlocks"),
                        bConfig.getStringList("requirements.command"),
                        bConfig.getConfigurationSection("requirements.levels")
                );

                buildings.put(buildingId, building);
                getLogger().info("✅ Загружено здание: " + buildingId +
                        " (Уровней: " + building.getLevels().size() +
                        ", Материал: " + building.getMaterial() + ")");

            } catch (Exception e) {
                getLogger().severe("❌ Ошибка загрузки здания " + buildingId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        getLogger().info("Попытка установки блока: " + item.getType());

        if (item.getType() != Material.END_STONE) {
            getLogger().info("Не END_STONE, пропускаем");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            getLogger().warning("У предмета нет меты или названия");
            return;
        }

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        getLogger().info("Поиск здания по имени: " + displayName);

        TownBuilding building = buildings.values().stream()
                .filter(b -> ChatColor.stripColor(b.getDisplayName()).equals(displayName))
                .findFirst()
                .orElse(null);

        if (building == null) {
            getLogger().warning("Здание не найдено для " + displayName);
            return;
        }

        Location loc = event.getBlock().getLocation();
        placedBuildings.put(loc, new BuildingData(
                building.getId(),
                0,
                event.getPlayer().getUniqueId(),
                loc
        ));

        getLogger().info("Блок установлен: " + building.getId() + " на " + locToString(loc));
        event.getPlayer().sendMessage(ChatColor.GREEN + "Вы установили " + building.getDisplayName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            getLogger().info("Не RIGHT_CLICK_BLOCK, пропускаем");
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            getLogger().info("Кликнут не блок");
            return;
        }

        Location loc = block.getLocation();
        BuildingData data = placedBuildings.get(loc);
        if (data == null) {
            getLogger().info("Нет данных для блока на " + locToString(loc));
            return;
        }

        getLogger().info("Открываем меню для блока: " + data.getBuildingId());
        event.setCancelled(true);
        openBuildingMenu(event.getPlayer(), data);
    }

    private void openBuildingMenu(Player player, BuildingData data) {
        TownBuilding building = buildings.get(data.getBuildingId());
        if (building == null) {
            getLogger().warning("Здание не найдено для ID: " + data.getBuildingId());
            player.sendMessage(ChatColor.RED + "Ошибка загрузки данных здания");
            return;
        }

        int nextLevel = data.getLevel() + 1;
        if (nextLevel > building.getMaxLevel()) {
            player.sendMessage(ChatColor.GREEN + "Это здание максимального уровня!");
            return;
        }

        BuildingLevel requirements = building.getLevels().get(nextLevel);
        if (requirements == null) {
            getLogger().warning("Нет требований для уровня " + nextLevel);
            return;
        }

        Inventory menu = Bukkit.createInventory(new MenuHolder(), 27,
                building.getDisplayName() + " Ур. " + nextLevel);

        // Границы
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, border);
            }
        }

        // Информация
        menu.setItem(4, createGuiItem(Material.BOOK,
                ChatColor.GOLD + "Требования для улучшения",
                ChatColor.GRAY + "Текущий уровень: " + ChatColor.YELLOW + data.getLevel(),
                ChatColor.GRAY + "Следующий уровень: " + ChatColor.GREEN + nextLevel));

        // Требуемые ресурсы
        int slot = 10;
        for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
            Material mat = entry.getKey();
            int required = entry.getValue();
            int has = countItems(player, mat);

            ChatColor color = has >= required ? ChatColor.GREEN : ChatColor.RED;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.RESET + mat.toString());

            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Нужно: " + color + required);
            lore.add(ChatColor.GRAY + "Есть: " + color + has);
            meta.setLore(lore);

            item.setItemMeta(meta);
            item.setAmount(Math.min(64, required));
            menu.setItem(slot++, item);
        }

        // Кнопка улучшения
        boolean canUpgrade = canUpgrade(player, requirements);
        Material buttonMat = canUpgrade ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
        String buttonName = canUpgrade ? ChatColor.GREEN + "Улучшить!" : ChatColor.RED + "Недостаточно ресурсов";

        menu.setItem(22, createGuiItem(buttonMat, buttonName,
                ChatColor.GRAY + "Нажмите для улучшения до",
                ChatColor.GRAY + "уровня " + nextLevel));

        player.openInventory(menu);
        activeSessions.put(player.getUniqueId(), data.getLocation());
        getLogger().info("Меню открыто для " + player.getName());
    }
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) {
            getLogger().info("Клик не в нашем меню");
            return;
        }

        event.setCancelled(true);
        getLogger().info("Обработка клика в меню");

        Location loc = activeSessions.get(player.getUniqueId());
        if (loc == null) {
            getLogger().warning("Нет активной сессии для " + player.getName());
            return;
        }

        BuildingData data = placedBuildings.get(loc);
        if (data == null) {
            getLogger().warning("Нет данных для локации " + locToString(loc));
            return;
        }

        if (event.getSlot() == 22) { // Кнопка улучшения
            handleUpgrade(player, data);
        }
    }

    private void handleUpgrade(Player player, BuildingData data) {
        TownBuilding building = buildings.get(data.getBuildingId());
        int nextLevel = data.getLevel() + 1;
        BuildingLevel requirements = building.getLevels().get(nextLevel);

        if (canUpgrade(player, requirements)) {
            // Списание ресурсов
            for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
                removeItems(player, entry.getKey(), entry.getValue());
            }

            data.setLevel(nextLevel);
            player.sendMessage(ChatColor.GREEN + building.getDisplayName() + " улучшена до уровня " + nextLevel);
            player.closeInventory();
            getLogger().info(player.getName() + " улучшил " + building.getId() + " до ур. " + nextLevel);
        } else {
            player.sendMessage(ChatColor.RED + "Недостаточно ресурсов для улучшения!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        BuildingData data = placedBuildings.get(loc);

        if (data == null) {
            getLogger().info("Блок не является зданием: " + locToString(loc));
            return;
        }

        event.setCancelled(true);
        data.incrementHits();
        TownBuilding building = buildings.get(data.getBuildingId());

        getLogger().info("Удар по зданию " + building.getId() +
                ". Ударов: " + data.getHitsTaken() + "/" + building.getHitsToBreak());

        if (data.getHitsTaken() >= building.getHitsToBreak()) {
            placedBuildings.remove(loc);
            block.setType(Material.AIR);
            event.getPlayer().sendMessage(ChatColor.RED + "Вы разрушили здание!");
            getLogger().info("Здание разрушено на " + locToString(loc));
        } else {
            event.getPlayer().sendMessage(ChatColor.YELLOW + "Прогресс разрушения: " +
                    data.getHitsTaken() + "/" + building.getHitsToBreak());
        }
    }

    // Вспомогательные методы
    private boolean canUpgrade(Player player, BuildingLevel requirements) {
        for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
            if (countItems(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private int countItems(Player player, Material material) {
        return Arrays.stream(player.getInventory().getContents())
                .filter(Objects::nonNull)
                .filter(item -> item.getType() == material)
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private void removeItems(Player player, Material material, int amount) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;

            int remove = Math.min(amount, item.getAmount());
            item.setAmount(item.getAmount() - remove);
            amount -= remove;

            if (amount <= 0) break;
        }
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("townhall")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Только игроки могут использовать эту команду!");
                return true;
            }

            Player player = (Player) sender;
            giveTownBuildingItem(player, "townhall");
            return true;
        }
        return false;
    }

    private void giveTownBuildingItem(Player player, String buildingId) {
        TownBuilding building = buildings.get(buildingId);
        if (building == null) {
            player.sendMessage(ChatColor.RED + "Это здание не найдено в конфигурации!");
            return;
        }

        ItemStack item = new ItemStack(building.getMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', building.getDisplayName()));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "ID: " + building.getId());
        lore.add(ChatColor.DARK_GRAY + "Поставьте блок и нажмите ПКМ");
        meta.setLore(lore);

        item.setItemMeta(meta);

        HashMap<Integer, ItemStack> failed = player.getInventory().addItem(item);
        if (!failed.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), failed.get(0));
        }
        player.sendMessage(ChatColor.GREEN + "Вы получили блок " + building.getDisplayName());
        getLogger().info("Выдан блок " + building.getId() + " игроку " + player.getName());
    }

    public static class MenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

