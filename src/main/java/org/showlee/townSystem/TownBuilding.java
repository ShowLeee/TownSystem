package org.showlee.townSystem;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Material;
import java.util.*;

public class TownBuilding {
    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final List<String> unlocks;
    private final List<String> commandRequirements;
    private final Map<Integer, BuildingLevel> levels;
    private final int hitsToBreak;
    private final Material material;

    public TownBuilding(String id, String displayName, int maxLevel,
                        List<String> unlocks, List<String> commandRequirements,
                        ConfigurationSection levelsConfig) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.unlocks = unlocks;
        this.commandRequirements = commandRequirements;
        this.levels = BuildingLevel.loadLevelsFromConfig(levelsConfig);
        this.hitsToBreak = levelsConfig.getInt("hits-to-break", 5);
        this.material = Material.valueOf(levelsConfig.getString("material", "END_STONE"));
    }

    // Геттеры
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public List<String> getUnlocks() { return unlocks; }
    public Map<Integer, BuildingLevel> getLevels() { return levels; }
    public int getHitsToBreak() { return hitsToBreak; }
    public Material getMaterial() { return material; }
}
