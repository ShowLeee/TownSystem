package org.showlee.townSystem.buildings;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class ChestListener implements Listener {
    private final TownHall townHall;

    public ChestListener(TownHall townHall) {
        this.townHall = townHall;
    }

    @EventHandler
    public void onChestPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.BARREL) return;

        if (!townHall.canPlaceChest(block.getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Здесь нельзя ставить сундуки!");
            return;
        }

        townHall.addChest(block.getLocation(), block);
    }
}
