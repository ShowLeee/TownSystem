package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Team;

public class TownBuilding implements Listener {

    private final TownSystem plugin;

    public TownBuilding(TownSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        Player player = event.getPlayer();

        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в команде для установки зданий!");
            event.setCancelled(true);
            return;
        }

        if (displayName.equals(plugin.getBuildingItemName())) {
            Block block = event.getBlock();

            BuildingData data = new BuildingData();
            data.setType("townhall");
            data.setLevel(0);
            data.setLocation(block.getLocation());
            data.setTeam(team.getName());
            data.setOwner(player.getUniqueId());

            plugin.addBuildingData(block.getLocation(), data);

            player.sendMessage(ChatColor.GREEN + "Вы установили Ратушу для команды " +
                    plugin.getTeamDisplayName(team.getName()));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.STRUCTURE_BLOCK) return;

        BuildingData data = plugin.getBuildingData(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        Team buildingTeam = player.getScoreboard().getTeam(data.getTeam());

        // Игрок не может ломать здания своей команды
        if (playerTeam != null && playerTeam.equals(buildingTeam)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Вы не можете ломать здания своей команды!");
            return;
        }

        // Разрешить ломать здания других команд
        plugin.removeBuildingData(block.getLocation());
        player.sendMessage(ChatColor.GREEN + "Вы разрушили здание команды " +
                plugin.getTeamDisplayName(data.getTeam()));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.STRUCTURE_BLOCK) return;

        BuildingData data = plugin.getBuildingData(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        Team buildingTeam = player.getScoreboard().getTeam(data.getTeam());

        // Проверка доступа к зданию
        if (buildingTeam != null && !buildingTeam.equals(playerTeam)) {
            player.sendMessage(ChatColor.RED + "Это здание принадлежит команде " +
                    plugin.getTeamDisplayName(data.getTeam()));
            event.setCancelled(true);
            return;
        }

        // Открываем меню здания
        MenuHolder menu = new MenuHolder(plugin, block.getLocation());
        menu.open(event.getPlayer());
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuHolder)) return;

        MenuHolder holder = (MenuHolder) event.getInventory().getHolder();
        holder.handleClick(event);
    }
    @EventHandler
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return;

        // Проверка защиты сундуков
        if (LocationUtil.isLocationProtected(block.getLocation(), event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "Этот сундук защищен командой!");
        }
    }
}