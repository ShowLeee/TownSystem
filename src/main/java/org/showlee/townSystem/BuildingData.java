package org.showlee.townSystem;

import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class BuildingData {
    private final String buildingId;
    private int level;
    private final UUID owner;
    private final Location location;
    private final List<String> unlocked;
    private int hitsTaken;

    // Основной конструктор
    public BuildingData(@NotNull String buildingId, int level, @NotNull UUID owner,
                        @NotNull Location location) {
        this(buildingId, level, owner, location, new ArrayList<>());
    }

    // Конструктор с unlocked
    public BuildingData(@NotNull String buildingId, int level, @NotNull UUID owner,
                        @NotNull Location location, @NotNull List<String> unlocked) {
        this.buildingId = buildingId;
        this.level = level;
        this.owner = owner;
        this.location = location;
        this.unlocked = new ArrayList<>(unlocked);
        this.hitsTaken = 0;
    }

    // Геттеры и сеттеры
    public String getBuildingId() { return buildingId; }
    public int getLevel() { return level; }
    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public List<String> getUnlocked() { return unlocked; }
    public int getHitsTaken() { return hitsTaken; }

    public void setLevel(int level) { this.level = level; }
    public void addUnlocked(String buildingId) { unlocked.add(buildingId); }
    public void incrementHits() { hitsTaken++; }
    public void resetHits() { hitsTaken = 0; }
}
