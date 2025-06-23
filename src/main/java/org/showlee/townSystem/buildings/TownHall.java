package org.showlee.townSystem.buildings;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;
import org.showlee.townSystem.TownSystem;

import java.util.*;

public class TownHall extends Building {
    private static final Map<Integer, TownHallLevel> LEVELS = new HashMap<>();

    static {
        LEVELS.put(1, new TownHallLevel(5, 6, Arrays.asList("mill", "mine")));
        LEVELS.put(2, new TownHallLevel(5, 10, Arrays.asList("smelter")));
        LEVELS.put(3, new TownHallLevel(7, 14, Arrays.asList("wizard_tower")));
        LEVELS.put(4, new TownHallLevel(10, 20, Arrays.asList("hell_altar")));
        LEVELS.put(5, new TownHallLevel(10, 28, Collections.emptyList()));
    }

    private final Map<Location, Block> chests = new HashMap<>();

    public TownHall(Location location, String team) {
        super(location, team);
    }

    @Override
    public void onPlace(Player player) {
        player.sendMessage(ChatColor.GREEN + "Вы построили ратушу!");
        // Запрещаем ставить сундуки в мире
        TownSystem.getInstance().getServer().getPluginManager()
                .registerEvents(new ChestListener(this), TownSystem.getInstance());
    }

    @Override
    public void onUpgrade(Player player) {
        TownHallLevel levelData = LEVELS.get(this.level);
        if (levelData == null) return;

        player.sendMessage(ChatColor.GOLD + "Ратуша улучшена до уровня " + this.level + "!");

        // Выдаем награды за уровень
        levelData.getRewardItems().forEach(itemType -> {
            ItemStack item = TownSystem.getInstance().getBuildingItem(itemType);
            player.getInventory().addItem(item);
        });
    }

    @Override
    public void onDestroy(Player player) {
        player.sendMessage(ChatColor.RED + "Ратуша разрушена!");
        chests.clear();
    }

    @Override
    public ItemStack getItem() {
        ItemStack item = new ItemStack(Material.END_STONE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Ратуша");
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Основное здание города",
                ChatColor.GRAY + "Уровень: " + ChatColor.GREEN + this.level
        ));
        item.setItemMeta(meta);
        return item;
    }

    public boolean canPlaceChest(Location location) {
        if (this.level == 0) return false;

        TownHallLevel levelData = LEVELS.get(this.level);
        double distance = location.distance(this.location);

        return distance <= levelData.getRadius() &&
                chests.size() < levelData.getMaxChests();
    }

    public void addChest(Location location, Block chest) {
        chests.put(location, chest);
    }

    public void removeChest(Location location) {
        chests.remove(location);
    }

    private static class TownHallLevel {
        private final int radius;
        private final int maxChests;
        private final List<String> rewardItems;

        public TownHallLevel(int radius, int maxChests, List<String> rewardItems) {
            this.radius = radius;
            this.maxChests = maxChests;
            this.rewardItems = rewardItems;
        }

        public int getRadius() { return radius; }
        public int getMaxChests() { return maxChests; }
        public List<String> getRewardItems() { return rewardItems; }
    }
}
