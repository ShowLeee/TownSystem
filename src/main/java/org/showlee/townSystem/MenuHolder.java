package org.showlee.townSystem;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

public class MenuHolder implements InventoryHolder {
    private final BuildingData buildingData;
    private final TownBuilding building;

    public MenuHolder(@NotNull BuildingData buildingData, @NotNull TownBuilding building) {
        this.buildingData = buildingData;
        this.building = building;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    @NotNull
    public BuildingData getBuildingData() {
        return this.buildingData;
    }

    @NotNull
    public TownBuilding getBuilding() {
        return this.building;
    }
}