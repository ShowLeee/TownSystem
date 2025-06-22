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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class TownSystem extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private Map<String, TownBuilding> buildings = new HashMap<>();
    private Map<Location, BuildingData> placedBuildings = new HashMap<>();
    private Map<UUID, Location> activeSessions = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        getServer().getPluginManager().registerEvents(this, this);
        loadBuildings();
        loadBuildingData();
        getLogger().info("TownSystem enabled!");
    }

    @Override
    public void onDisable() {
        saveBuildingData();
        getLogger().info("TownSystem disabled!");
    }

    private void loadBuildings() {
        ConfigurationSection buildingsSection = config.getConfigurationSection("buildings");
        if (buildingsSection == null) return;

        for (String buildingId : buildingsSection.getKeys(false)) {
            String path = "buildings." + buildingId;

            TownBuilding building = new TownBuilding(
                    buildingId,
                    config.getString(path + ".display-name"),
                    config.getInt(path + ".max-level"),
                    config.getStringList(path + ".unlocks"),
                    config.getStringList(path + ".requirements.command"),
                    config.getConfigurationSection(path + ".requirements.levels")
            );

            buildings.put(buildingId, building);
        }
    }

    private void loadBuildingData() {
        if (!config.isConfigurationSection("placed-buildings")) return;

        for (String key : config.getConfigurationSection("placed-buildings").getKeys(false)) {
            String path = "placed-buildings." + key;
            Location location = deserializeLocation(key);
            if (location == null) continue;

            BuildingData data = new BuildingData(
                    config.getString(path + ".building-id"),
                    config.getInt(path + ".level"),
                    UUID.fromString(config.getString(path + ".owner")),
                    location,
                    config.getStringList(path + ".unlocked")
            );

            placedBuildings.put(location, data);
        }
    }

    private void saveBuildingData() {
        config.set("placed-buildings", null);

        for (Map.Entry<Location, BuildingData> entry : placedBuildings.entrySet()) {
            String locationKey = serializeLocation(entry.getKey());
            String path = "placed-buildings." + locationKey;

            config.set(path + ".building-id", entry.getValue().getBuildingId());
            config.set(path + ".level", entry.getValue().getLevel());
            config.set(path + ".owner", entry.getValue().getOwner().toString());
            config.set(path + ".unlocked", new ArrayList<>(entry.getValue().getUnlocked()));
        }

        saveConfig();
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    private Location deserializeLocation(String s) {
        String[] parts = s.split(";");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
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

            if (args.length == 0) {
                giveTownBuildingItem(player, "townhall");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload") && player.hasPermission("townsystem.admin")) {
                reloadConfig();
                config = getConfig();
                buildings.clear();
                loadBuildings();
                player.sendMessage(ChatColor.GREEN + "Конфигурация перезагружена!");
                return true;
            }
        }
        return false;
    }

    private void giveTownBuildingItem(Player player, String buildingId) {
        TownBuilding building = buildings.get(buildingId);
        if (building == null) return;

        ItemStack item = new ItemStack(Material.END_STONE);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(building.getDisplayName());

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Тип: " + ChatColor.YELLOW + building.getId());
        lore.add(ChatColor.DARK_GRAY + "Поставьте блок и нажмите ПКМ");
        meta.setLore(lore);

        item.setItemMeta(meta);

        HashMap<Integer, ItemStack> failed = player.getInventory().addItem(item);
        if (!failed.isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), failed.get(0));
        }

        player.sendMessage(ChatColor.GREEN + "Вы получили блок " + building.getDisplayName() + ChatColor.GREEN + "!");
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item.getType() != Material.END_STONE) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        TownBuilding building = getBuildingByDisplayName(meta.getDisplayName());
        if (building == null) return;

        if (placedBuildings.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
            return;
        }

        placedBuildings.put(event.getBlock().getLocation(),
                new BuildingData(
                        building.getId(),
                        0,
                        event.getPlayer().getUniqueId(),
                        event.getBlock().getLocation(),
                        new ArrayList<>()
                ));

        event.getPlayer().sendMessage(ChatColor.GREEN + "Вы установили " + building.getDisplayName() + ChatColor.GREEN + "!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND ||
                event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_STONE) return;

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
            player.sendMessage(ChatColor.GREEN + "Это здание уже максимального уровня!");
            return;
        }

        BuildingLevel requirements = building.getLevels().get(nextLevel);
        Inventory menu = Bukkit.createInventory(
                new MenuHolder(data, building),
                27,
                building.getDisplayName() + " Ур. " + nextLevel
        );

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

        activeSessions.put(player.getUniqueId(), data.getLocation());
        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof MenuHolder)) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        MenuHolder menuHolder = (MenuHolder) holder;
        BuildingData data = menuHolder.getBuildingData();
        TownBuilding building = menuHolder.getBuilding();

        if (event.getSlot() == 22) {
            int nextLevel = data.getLevel() + 1;
            BuildingLevel requirements = building.getLevels().get(nextLevel);

            if (canUpgrade(player, requirements)) {
                takeResources(player, requirements);
                data.setLevel(nextLevel);

                if (nextLevel == 1 && building.getId().equals("townhall")) {
                    giveUnlockedBuildings(player, building);
                }

                player.sendMessage(ChatColor.GREEN + building.getDisplayName() + " улучшена до уровня " + nextLevel + "!");
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                openBuildingMenu(player, data);
            } else {
                player.sendMessage(ChatColor.RED + "Недостаточно ресурсов для улучшения!");
            }
        }
    }

    private void giveUnlockedBuildings(Player player, TownBuilding building) {
        for (String unlockedId : building.getUnlocks()) {
            giveTownBuildingItem(player, unlockedId);
            player.sendMessage(ChatColor.GREEN + "Вы получили блок " +
                    buildings.get(unlockedId).getDisplayName() + "!");
        }
    }

    private boolean canUpgrade(Player player, BuildingLevel requirements) {
        for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
            if (countItems(player, entry.getKey()) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void takeResources(Player player, BuildingLevel requirements) {
        for (Map.Entry<Material, Integer> entry : requirements.getRequiredItems().entrySet()) {
            removeItems(player, entry.getKey(), entry.getValue());
        }
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

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private TownBuilding getBuildingByDisplayName(String displayName) {
        return buildings.values().stream()
                .filter(b -> b.getDisplayName().equals(displayName))
                .findFirst()
                .orElse(null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.END_STONE &&
                placedBuildings.containsKey(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Этот блок нельзя сломать!");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            activeSessions.remove(event.getPlayer().getUniqueId());
        }
    }
}

