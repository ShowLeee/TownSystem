package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class MenuHolder implements InventoryHolder {
    private final Inventory inventory;
    private final Location location;
    private final TownSystem plugin;
    private final BuildingData buildingData;
    private final ConfigurationSection buildingConfig;

    public MenuHolder(TownSystem plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
        this.buildingData = plugin.getBuildingData(location);
        this.buildingConfig = plugin.getConfig().getConfigurationSection("buildings." + buildingData.getType());
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.translateAlternateColorCodes('&',
                buildingConfig.getString("levels." + buildingData.getLevel() + ".display-name")));
        setupItems();
    }

    private void setupItems() {
        // Границы меню
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        // Информация о здании
        ConfigurationSection levelConfig = buildingConfig.getConfigurationSection("levels." + buildingData.getLevel());

        // Иконка здания
        ItemStack buildingInfo = createGuiItem(
                Material.STRUCTURE_BLOCK,
                ChatColor.GOLD + buildingConfig.getString("display-name"),
                ChatColor.GRAY + "Уровень: " + ChatColor.YELLOW + buildingData.getLevel(),
                ChatColor.GRAY + "Тип: " + ChatColor.WHITE + buildingData.getType()
        );
        inventory.setItem(4, buildingInfo);

        // Кнопка ресурсов
        ItemStack resourcesBtn = createGuiItem(
                Material.BOOK,
                ChatColor.YELLOW + "Требуемые ресурсы",
                getResourcesLore(levelConfig)
        );
        inventory.setItem(11, resourcesBtn);

        // Кнопка сдачи ресурсов
        ItemStack submitBtn = createGuiItem(
                Material.HOPPER,
                ChatColor.GREEN + "Сдать ресурсы",
                "",
                ChatColor.GRAY + "Нажмите, чтобы сдать ресурсы",
                ChatColor.GRAY + "для улучшения здания"
        );
        inventory.setItem(13, submitBtn);

        // Кнопка наград
        ItemStack rewardsBtn = createGuiItem(
                Material.CHEST,
                ChatColor.GREEN + "Награды за улучшение",
                getRewardsLore(levelConfig)
        );
        inventory.setItem(15, rewardsBtn);

        // Кнопка закрытия
        ItemStack closeBtn = createGuiItem(
                Material.BARRIER,
                ChatColor.RED + "Закрыть"
        );
        inventory.setItem(22, closeBtn);
    }

    private String[] getResourcesLore(ConfigurationSection levelConfig) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Необходимо для улучшения:");

        if (levelConfig.contains("resources")) {
            ConfigurationSection resources = levelConfig.getConfigurationSection("resources");
            ConfigurationSection materialNames = plugin.getConfig().getConfigurationSection("material-names");

            for (String material : resources.getKeys(false)) {
                String displayName = materialNames.getString(material, material); // Получаем русское название
                lore.add(ChatColor.WHITE + "- " + displayName + ": " + resources.getInt(material));
            }
        }

        return lore.toArray(new String[0]);
    }

    private String[] getRewardsLore(ConfigurationSection levelConfig) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Вы получите:");

        if (levelConfig.contains("rewards")) {
            ConfigurationSection rewards = levelConfig.getConfigurationSection("rewards");
            ConfigurationSection materialNames = plugin.getConfig().getConfigurationSection("material-names");

            for (String material : rewards.getKeys(false)) {
                String displayName = materialNames.getString(material, material);
                lore.add(ChatColor.WHITE + "- " + displayName + ": " + rewards.getInt(material));
            }
        }

        return lore.toArray(new String[0]);
    }

    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
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

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 13: // Кнопка сдачи ресурсов
                handleResourceSubmit(player);
                break;
            case 22: // Кнопка закрытия
                player.closeInventory();
                break;
        }
    }

    private void handleResourceSubmit(Player player) {
        ConfigurationSection levelConfig = buildingConfig.getConfigurationSection("levels." + buildingData.getLevel());

        // Проверка ресурсов
        if (!hasRequiredResources(player, levelConfig.getConfigurationSection("resources"))) {
            player.sendMessage(ChatColor.RED + "У вас недостаточно ресурсов для улучшения!");
            return;
        }

        // Забрать ресурсы
        takeResources(player, levelConfig.getConfigurationSection("resources"));

        // Улучшить здание
        buildingData.setLevel(buildingData.getLevel() + 1);
        plugin.updateBuildingData(buildingData);

        // Выдать награды
        giveRewards(player, levelConfig.getConfigurationSection("rewards"));

        player.sendMessage(ChatColor.GREEN + "Здание улучшено до уровня " + buildingData.getLevel() + "!");
        player.closeInventory();
    }

    private boolean hasRequiredResources(Player player, ConfigurationSection resources) {
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

    private void takeResources(Player player, ConfigurationSection resources) {
        if (resources == null) return;

        for (String material : resources.getKeys(false)) {
            Material mat = Material.matchMaterial(material);
            if (mat == null) continue;

            int amount = resources.getInt(material);
            for (ItemStack item : player.getInventory()) {
                if (item != null && item.getType() == mat) {
                    int remove = Math.min(amount, item.getAmount());
                    item.setAmount(item.getAmount() - remove);
                    amount -= remove;
                    if (amount <= 0) break;
                }
            }
        }
    }

    private void giveRewards(Player player, ConfigurationSection rewards) {
        if (rewards == null) return;

        for (String material : rewards.getKeys(false)) {
            Material mat = Material.matchMaterial(material);
            if (mat == null) continue;

            player.getInventory().addItem(new ItemStack(mat, rewards.getInt(material)));
        }
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

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}