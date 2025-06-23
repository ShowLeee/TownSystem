package org.showlee.townSystem;

import org.bukkit.Location;
import java.util.UUID;

public class BuildingData {
    private String type;
    private int level;
    private UUID owner;
    private Location location;
    private String team;

    // Конструктор по умолчанию
    public BuildingData() {
    }

    // Геттеры и сеттеры
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }
}