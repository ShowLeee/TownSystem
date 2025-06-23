package org.showlee.townSystem;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.util.*;

public class TownSystem extends JavaPlugin {

    private static TownSystem instance;
    private Map<Location, BuildingData> buildingDataMap = new HashMap<>();
    private Map<String, String> teamDisplayNames = new HashMap<>();
    private String buildingItemName;
    private BuildingManager buildingManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        reloadConfig();

        buildingManager = new BuildingManager();
        Objects.requireNonNull(this.getCommand("giveratusha")).setExecutor(new GiveRatushaCommand(this));
        // Загрузка настроек

        FileConfiguration config = getConfig();
        buildingItemName = ChatColor.translateAlternateColorCodes('&',
                config.getString("building-item-name", "&6Ратуша"));

        // Загрузка команд
        ConfigurationSection teamsSection = config.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String team : teamsSection.getKeys(false)) {
                teamDisplayNames.put(team, ChatColor.translateAlternateColorCodes('&',
                        teamsSection.getString(team + ".display_name", team)));
            }
        }

        // Регистрация событий
        getServer().getPluginManager().registerEvents(new TownBuilding(this), this);
    }

    @Override
    public void onDisable() {
        saveBuildingData();

    }

    public static TownSystem getInstance() {
        return instance;
    }
    public BuildingManager getBuildingManager() {
        return buildingManager;
    }
    public String getBuildingItemName() {
        return buildingItemName;
    }
    public ItemStack getBuildingItem(String type) {
        // Возвращает ItemStack для указанного типа здания
        return null;
    }
    public String getTeamDisplayName(String team) {
        return teamDisplayNames.getOrDefault(team, team);
    }
    public boolean isProgressiveBreakingEnabled() {
        return getConfig().getBoolean("block-breaking.enabled", true);
    }
    public void addBuildingData(Location location, BuildingData data) {
        buildingDataMap.put(location, data);
    }
    public Map<Material, Integer> getRequiredResources(String buildingType, int level) {
        ConfigurationSection levelSection = getConfig()
                .getConfigurationSection("building-levels." + buildingType + "." + level);

        Map<Material, Integer> resources = new HashMap<>();
        if (levelSection != null) {
            ConfigurationSection resourcesSection = levelSection.getConfigurationSection("required-resources");
            if (resourcesSection != null) {
                for (String material : resourcesSection.getKeys(false)) {
                    resources.put(Material.matchMaterial(material), resourcesSection.getInt(material));
                }
            }
        }
        return resources;
    }

    public List<String> getRewards(String buildingType, int level) {
        return getConfig()
                .getStringList("building-levels." + buildingType + "." + level + ".reward");
    }

    public void updateBuildingData(BuildingData data) {
        // Сохраняем обновленные данные здания
        buildingDataMap.put(data.getLocation(), data);
        // Здесь можно добавить сохранение в файл
    }
    public BuildingData getBuildingData(Location location) {
        return buildingDataMap.get(location);
    }

    public void removeBuildingData(Location location) {
        buildingDataMap.remove(location);
    }

    public Map<Location, BuildingData> getAllBuildingData() {
        return Collections.unmodifiableMap(buildingDataMap);
    }

    private void saveBuildingData() {
        // Реализация сохранения данных зданий
    }

    private void loadBuildingData() {
        // Реализация загрузки данных зданий
    }
}