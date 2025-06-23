package org.showlee.townSystem;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class TownBuilding {
    private final String id;
    private final String displayName;
    private final int maxLevel;
    private final List<String> unlocks;
    private final List<String> commandRequirements;
    private final Map<Integer, BuildingLevel> levels;
    private final int hitsToBreak;

    public TownBuilding(@NotNull String id, @NotNull String displayName, int maxLevel,
                        @NotNull List<String> unlocks, @NotNull List<String> commandRequirements,
                        @NotNull ConfigurationSection config) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.unlocks = unlocks;
        this.commandRequirements = commandRequirements;
        this.levels = BuildingLevel.loadLevelsFromConfig(config.getConfigurationSection("levels"));
        this.hitsToBreak = config.getInt("hits-to-break", 3);
    }

    // Геттеры
    public @NotNull String getId() { return id; }
    public @NotNull String getDisplayName() { return displayName; }
    public int getMaxLevel() { return maxLevel; }
    public @NotNull List<String> getUnlocks() { return unlocks; }
    public @NotNull List<String> getCommandRequirements() { return commandRequirements; }
    public @NotNull Map<Integer, BuildingLevel> getLevels() { return levels; }
    public int getHitsToBreak() { return hitsToBreak; }
}
