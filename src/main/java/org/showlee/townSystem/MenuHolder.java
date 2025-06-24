package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;
import org.showlee.townSystem.buildings.TownHall;

import java.util.*;

public class MenuHolder implements InventoryHolder {
    private final Inventory inventory;
    private final Location location;
    private final TownSystem plugin;
    private final TownHall townHall;
    private final BuildingData buildingData;
    private final ConfigurationSection buildingConfig;

    public MenuHolder(TownSystem plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
        this.townHall = plugin.getBuildingManager().getTownHallAt(location);
        this.buildingData = plugin.getBuildingData(location);
        this.buildingConfig = plugin.getConfig().getConfigurationSection("buildings." + buildingData.getType());
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.translateAlternateColorCodes('&',
                buildingConfig.getString("levels." + buildingData.getLevel() + ".display-name")));

        setupItems();
    }

    private void setupItems() {
        // Границы
        ItemStack border = createBorderItem();
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        // Информация о ратуше
        inventory.setItem(4, createInfoItem());

        // Кнопка улучшения
        if (townHall.getLevel() < 5) {
            inventory.setItem(22, createUpgradeItem());
        }

        // Кнопка закрытия
        inventory.setItem(26, createCloseItem());
    }

    private ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        return border;
    }

    private ItemStack createInfoItem() {
        ConfigurationSection levelConfig = getCurrentLevelConfig();

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName(ChatColor.GOLD + levelConfig.getString("display-name"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Уровень: " + ChatColor.YELLOW + townHall.getLevel());
        lore.add(ChatColor.GRAY + "Команда: " + ChatColor.YELLOW + townHall.getTeam());
        lore.add(ChatColor.GRAY + "Сундуков: " + ChatColor.YELLOW +
                townHall.getCurrentChestCount() + "/" + levelConfig.getInt("chest-limit"));
        lore.add(ChatColor.GRAY + "Радиус: " + ChatColor.YELLOW + levelConfig.getInt("radius") + " блоков");

        infoMeta.setLore(lore);
        infoItem.setItemMeta(infoMeta);
        return infoItem;
    }

    private ItemStack createUpgradeItem() {
        int nextLevel = townHall.getLevel() + 1;
        ConfigurationSection nextLevelConfig = getLevelConfig(nextLevel);

        ItemStack upgradeItem = new ItemStack(Material.ANVIL);
        ItemMeta upgradeMeta = upgradeItem.getItemMeta();
        upgradeMeta.setDisplayName(ChatColor.GREEN + "Улучшить до " + nextLevelConfig.getString("display-name"));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Требуется:");

        // Добавляем ресурсы для улучшения
        ConfigurationSection resources = nextLevelConfig.getConfigurationSection("resources");
        if (resources != null) {
            for (String material : resources.getKeys(false)) {
                String materialName = plugin.getConfig().getString("material-names." + material, material);
                lore.add(ChatColor.WHITE + "- " + materialName + ": " + resources.getInt(material));
            }
        }

        lore.add("");
        lore.add(ChatColor.GRAY + "Награда:");
        for (String reward : nextLevelConfig.getStringList("rewards")) {
            String[] parts = reward.split(":");
            String buildingName = parts[0];
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            String displayName = plugin.getBuildingDisplayName(buildingName);
            lore.add(ChatColor.GREEN + "- " + displayName + (amount > 1 ? " x" + amount : ""));
        }

        upgradeMeta.setLore(lore);
        upgradeItem.setItemMeta(upgradeMeta);
        return upgradeItem;
    }

    private ItemStack createCloseItem() {
        ItemStack closeItem = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Закрыть");
        closeItem.setItemMeta(closeMeta);
        return closeItem;
    }

    private ConfigurationSection getCurrentLevelConfig() {
        return getLevelConfig(townHall.getLevel());
    }

    private ConfigurationSection getLevelConfig(int level) {
        return plugin.getConfig()
                .getConfigurationSection("buildings.townhall.levels." + level);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        if (slot == 22 && event.getCurrentItem() != null &&
                event.getCurrentItem().getType() == Material.ANVIL) {
            attemptUpgrade(player);
        } else if (slot == 26) {
            player.closeInventory();
        }
    }

    private void attemptUpgrade(Player player) {
        int nextLevel = townHall.getLevel() + 1;
        ConfigurationSection nextLevelConfig = getLevelConfig(nextLevel);

        if (!hasResources(player, nextLevelConfig.getConfigurationSection("resources"))) {
            player.sendMessage(ChatColor.RED + "Не хватает ресурсов для улучшения!");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        upgradeTownHall(player, nextLevel, nextLevelConfig);
    }

    private boolean hasResources(Player player, ConfigurationSection resources) {
        if (resources == null) return true;

        for (String material : resources.getKeys(false)) {
            Material mat = Material.matchMaterial(material);
            if (mat == null) continue;

            if (countItems(player, mat) < resources.getInt(material)) {
                return false;
            }
        }
        return true;
    }

    private void upgradeTownHall(Player player, int newLevel, ConfigurationSection levelConfig) {
        // Забираем ресурсы
        ConfigurationSection resources = levelConfig.getConfigurationSection("resources");
        if (resources != null) {
            for (String material : resources.getKeys(false)) {
                Material mat = Material.matchMaterial(material);
                if (mat != null) {
                    removeItems(player, mat, resources.getInt(material));
                }
            }
        }

        // Выполняем улучшение
        plugin.getBuildingManager().upgradeBuilding(location, player);

        // Выдаем награды
        giveRewards(player, levelConfig.getStringList("rewards"));

        // Эффекты
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GREEN + "Ратуша улучшена до уровня " + newLevel + "!");

        // Обновляем меню
        setupItems();
    }

    private void giveRewards(Player player, List<String> rewards) {
        for (String reward : rewards) {
            String[] parts = reward.split(":");
            String buildingName = parts[0];
            int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;

            ItemStack item = plugin.getBuildingItem(buildingName);
            item.setAmount(amount);
            player.getInventory().addItem(item);
        }
    }
    public void open(Player player) {
        if (buildingData == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: данные здания не найдены!");
            return;
        }

        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        Team buildingTeam = player.getScoreboard().getTeam(buildingData.getTeam());

        if (buildingTeam != null && !buildingTeam.equals(playerTeam)) {
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с зданиями чужой команды!");
            return;
        }

        player.openInventory(inventory);
    }
    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private void removeItems(Player player, Material material, int amount) {
        for (ItemStack item : player.getInventory()) {
            if (item != null && item.getType() == material) {
                int remove = Math.min(amount, item.getAmount());
                item.setAmount(item.getAmount() - remove);
                amount -= remove;
                if (amount <= 0) break;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}