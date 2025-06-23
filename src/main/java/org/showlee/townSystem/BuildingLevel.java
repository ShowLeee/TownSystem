package org.showlee.townSystem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public class BuildingLevel {
    private final Map<Material, Integer> requiredItems;
    private final int requiredMoney;
    private final List<String> requiredPermissions;

    public BuildingLevel(Map<Material, Integer> requiredItems, int requiredMoney,
                         List<String> requiredPermissions) {
        this.requiredItems = requiredItems;
        this.requiredMoney = requiredMoney;
        this.requiredPermissions = requiredPermissions;
    }

    public static Map<Integer, BuildingLevel> loadLevelsFromConfig(ConfigurationSection levelsConfig) {
        Map<Integer, BuildingLevel> levels = new HashMap<>();
        if (levelsConfig == null) return levels;

        for (String levelStr : levelsConfig.getKeys(false)) {
            int level = Integer.parseInt(levelStr);
            ConfigurationSection levelSection = levelsConfig.getConfigurationSection(levelStr);

            Map<Material, Integer> items = new HashMap<>();
            for (String item : levelSection.getStringList("items")) {
                String[] parts = item.split(":");
                items.put(Material.valueOf(parts[0]), Integer.parseInt(parts[1]));
            }

            levels.put(level, new BuildingLevel(
                    items,
                    levelSection.getInt("money", 0),
                    levelSection.getStringList("permissions")
            ));
        }
        return levels;
    }

    // Геттеры
    public Map<Material, Integer> getRequiredItems() { return requiredItems; }
    public int getRequiredMoney() { return requiredMoney; }
    public List<String> getRequiredPermissions() { return requiredPermissions; }
}
