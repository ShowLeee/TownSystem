package org.showlee.townSystem.buildings;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Building {
    protected final Location location;
    protected int level;
    protected String team;

    public Building(Location location, String team) {
        this.location = location;
        this.level = 0;
        this.team = team;
    }

    public abstract void onPlace(Player player);
    public abstract void onUpgrade(Player player);
    public abstract void onDestroy(Player player);
    public abstract ItemStack getItem();

    // Геттеры и сеттеры
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getTeam() { return team; }
    public Location getLocation() { return location; }
}
