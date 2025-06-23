package org.showlee.townSystem;

import org.bukkit.Location;
import java.util.UUID;

public class BuildingData {
    private final String buildingId;
    private int level;
    private final UUID owner;
    private final Location location;
    private int hitsTaken;

    public BuildingData(String buildingId, int level, UUID owner, Location location) {
        this.buildingId = buildingId;
        this.level = level;
        this.owner = owner;
        this.location = location;
        this.hitsTaken = 0;
    }

    // Геттеры и сеттеры
    public String getBuildingId() { return buildingId; }
    public int getLevel() { return level; }
    public UUID getOwner() { return owner; }
    public Location getLocation() { return location; }
    public int getHitsTaken() { return hitsTaken; }

    public void setLevel(int level) { this.level = level; }
    public void incrementHits() { hitsTaken++; }
    public void resetHits() { hitsTaken = 0; }
}
