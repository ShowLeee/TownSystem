package org.showlee.townSystem;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.showlee.townSystem.buildings.Building;
import org.showlee.townSystem.buildings.TownHall;

import java.util.HashMap;
import java.util.Map;

public class BuildingManager {
    private final Map<Location, Building> buildings = new HashMap<>();

    public void addBuilding(Location location, Building building) {
        buildings.put(location, building);
        building.onPlace(null); // Можно передать игрока, если нужно
    }

    public TownHall getTownHallAt(Location location) {
        Building building = buildings.get(location);
        return building instanceof TownHall ? (TownHall) building : null;
    }

    public void upgradeBuilding(Location location, Player player) {
        Building building = buildings.get(location);
        if (building != null) {
            building.setLevel(building.getLevel() + 1);
            building.onUpgrade(player);
        }
    }
}
