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
        getLogger().info("TownSystem включен!");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.END_STONE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        String displayName = ChatColor.stripColor(meta.getDisplayName());
        TownBuilding building = buildings.values().stream()
                .filter(b -> ChatColor.stripColor(b.getDisplayName()).equals(displayName))
                .findFirst()
                .orElse(null);

        if (building == null) return;

        placedBuildings.put(event.getBlock().getLocation(),
                new BuildingData(
                        building.getId(),
                        0,
                        event.getPlayer().getUniqueId(),
                        event.getBlock().getLocation()
                ));

        event.getPlayer().sendMessage(ChatColor.GREEN + "Вы установили " + building.getDisplayName());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        BuildingData data = placedBuildings.get(block.getLocation());
        if (data == null) return;

        event.setCancelled(true);
        openBuildingMenu(event.getPlayer(), data);
    }

    private void openBuildingMenu(Player player, BuildingData data) {
        TownBuilding building = buildings.get(data.getBuildingId());
        if (building == null) return;

        int nextLevel = data.getLevel() + 1;
        if (nextLevel > building.getMaxLevel()) {
            player.sendMessage(ChatColor.GREEN + "Это здание максимального уровня!");
            return;
        }

        BuildingLevel requirements = building.getLevels().get(nextLevel);
        Inventory menu = Bukkit.createInventory(new MenuHolder(), 27,
                building.getDisplayName() + " Ур. " + nextLevel);

        // Заполняем границы
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i > 17 || i % 9 == 0 || i % 9 == 8) {
                menu.setItem(i, border);
            }
        }

        // Информация
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + "Требования для улучшения");
        infoMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Текущий уровень: " + data.getLevel(),
                ChatColor.GRAY + "Следующий уровень: " + nextLevel
        ));
        info.setItemMeta(infoMeta);
        menu.setItem(4, info);

        // Требуемые предметы
        int slot = 10;
        for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
            ItemStack reqItem = new ItemStack(entry.getKey());
            ItemMeta meta = reqItem.getItemMeta();
            meta.setDisplayName(entry.getKey().toString());
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Нужно: " + entry.getValue(),
                    ChatColor.GRAY + "У вас: " + countItems(player, entry.getKey())
            ));
            reqItem.setItemMeta(meta);
            menu.setItem(slot++, reqItem);
        }

        // Кнопка улучшения
        ItemStack upgradeBtn = new ItemStack(canUpgrade(player, requirements) ?
                Material.LIME_CONCRETE : Material.RED_CONCRETE);
        ItemMeta btnMeta = upgradeBtn.getItemMeta();
        btnMeta.setDisplayName(canUpgrade(player, requirements) ?
                ChatColor.GREEN + "Улучшить!" : ChatColor.RED + "Недостаточно ресурсов");
        btnMeta.setLore(Collections.singletonList(
                ChatColor.GRAY + "Нажмите для улучшения"
        ));
        upgradeBtn.setItemMeta(btnMeta);
        menu.setItem(22, upgradeBtn);

        player.openInventory(menu);
        activeSessions.put(player.getUniqueId(), data.getLocation());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        Location loc = activeSessions.get(player.getUniqueId());
        if (loc == null) return;

        BuildingData data = placedBuildings.get(loc);
        if (data == null) return;

        TownBuilding building = buildings.get(data.getBuildingId());
        if (building == null) return;

        if (event.getSlot() == 22) {
            handleUpgrade(player, data, building);
        }
    }

    private void handleUpgrade(Player player, BuildingData data, TownBuilding building) {
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
        } else {
            player.sendMessage(ChatColor.RED + "Недостаточно ресурсов для улучшения!");
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        BuildingData data = placedBuildings.get(block.getLocation());
        if (data == null) return;

        event.setCancelled(true);
        data.incrementHits();
        TownBuilding building = buildings.get(data.getBuildingId());

        // Эффекты при ударе
        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_STONE_HIT, 1.0f, 1.0f);
        block.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, block.getLocation().add(0.5, 0.5, 0.5),
                10, block.getBlockData());

        if (data.getHitsTaken() >= building.getHitsToBreak()) {
            placedBuildings.remove(block.getLocation());
            block.setType(Material.AIR);
            event.getPlayer().sendMessage(ChatColor.RED + "Здание разрушено!");
            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1.0f, 1.0f);
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

    // Другие необходимые методы...
    private void loadBuildings() {
        ConfigurationSection buildingsSection = config.getConfigurationSection("buildings");
        if (buildingsSection == null) {
            getLogger().warning("Не найдена секция buildings в config.yml!");
            return;
        }

        for (String buildingId : buildingsSection.getKeys(false)) {
            ConfigurationSection bConfig = buildingsSection.getConfigurationSection(buildingId);
            if (bConfig == null) continue;

            TownBuilding building = new TownBuilding(
                    buildingId,
                    bConfig.getString("display-name", buildingId),
                    bConfig.getInt("max-level", 1),
                    bConfig.getStringList("unlocks"),
                    bConfig.getStringList("requirements.command"),
                    bConfig.getConfigurationSection("requirements.levels")
            );
            buildings.put(buildingId, building);
        }
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

        ItemStack item = new ItemStack(Material.END_STONE);
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
    }

    public static class MenuHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

