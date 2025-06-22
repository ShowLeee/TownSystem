package org.showlee.townSystem;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildingLevel {
    private final Map<Material, Integer> requiredItems;
    private final int requiredMoney;
    private final List<String> requiredPermissions;

    public BuildingLevel(@NotNull Map<Material, Integer> requiredItems,
                         int requiredMoney,
                         @NotNull List<String> requiredPermissions) {
        this.requiredItems = requiredItems;
        this.requiredMoney = requiredMoney;
        this.requiredPermissions = requiredPermissions;
    }

    public static @NotNull Map<Integer, BuildingLevel> loadLevelsFromConfig(
            @NotNull ConfigurationSection levelsConfig) {
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
    public @NotNull Map<Material, Integer> getRequiredItems() { return requiredItems; }
    public int getRequiredMoney() { return requiredMoney; }
    public @NotNull List<String> getRequiredPermissions() { return requiredPermissions; }
}
