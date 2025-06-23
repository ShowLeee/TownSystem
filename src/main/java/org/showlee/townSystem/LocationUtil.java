package org.showlee.townSystem;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

public class LocationUtil {
    public static String serialize(Location loc) {
        return loc.getWorld().getName() + ";" +
                loc.getBlockX() + ";" +
                loc.getBlockY() + ";" +
                loc.getBlockZ();
    }
    public static boolean isLocationProtected(Location loc, Player player) {
        TownSystem townSystem = TownSystem.getInstance();

        for (BuildingData data : townSystem.getAllBuildingData().values()) {
            if (data.getLocation() != null &&
                    data.getLocation().getWorld().equals(loc.getWorld()) &&
                    data.getLocation().distance(loc) <= 10) {

                Team playerTeam = player.getScoreboard().getPlayerTeam(player);
                Team buildingTeam = player.getScoreboard().getTeam(data.getTeam());

                if (buildingTeam != null && !buildingTeam.equals(playerTeam)) {
                    return true;
                }
            }
        }
        return false;
    }
    public static Location deserialize(String s) {
        String[] parts = s.split(";");
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        return new Location(
                world,
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
        );
    }
}
