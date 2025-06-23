package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TownBuilding implements Listener {
    private final TownSystem plugin;
    private final Map<Player, BlockBreakData> breakingPlayers = new HashMap<>();
    private final Map<Location, BossBar> hpBars = new HashMap<>();
    private final Map<Location, Integer> buildingHP = new HashMap<>();
    private final Map<Location, Block> originalBlocks = new HashMap<>();
    private final Map<Player, BukkitTask> pendingDamage = new HashMap<>();
    private BukkitTask bossBarUpdateTask;
    private static final int VIEW_DISTANCE = 5;
    public TownBuilding(TownSystem plugin) {
        this.plugin = plugin;
        startBossBarUpdater();
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;

        String displayName = item.getItemMeta().getDisplayName();
        Player player = event.getPlayer();

        Team team = event.getPlayer().getScoreboard().getPlayerTeam(event.getPlayer());
        if (team == null) {
            player.sendMessage(ChatColor.RED + "Вы должны состоять в команде для установки зданий!");
            event.setCancelled(true);
            return;
        }

        if (item.getItemMeta().getDisplayName().equals(plugin.getBuildingItemName())) {
            Block block = event.getBlock();
            BuildingData data = new BuildingData();
            data.setType("townhall");
            data.setLevel(0);
            data.setLocation(block.getLocation());

            if (team != null) {
                data.setTeam(team.getName());
            }
            plugin.addBuildingData(block.getLocation(), data);
            initBuildingHP(block);
            player.sendMessage(ChatColor.GREEN + "Вы установили Ратушу для команды " +
                    plugin.getTeamDisplayName(team.getName()));

        }
    }
    private void startBossBarUpdater() {
        // Обновляем видимость боссбаров каждую секунду
        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateVisibleBossBars(player);
            }
        }, 0L, 20L);
    }
    private void updateVisibleBossBars(Player player) {
        // Для каждого боссбара проверяем расстояние
        for (Map.Entry<Location, BossBar> entry : hpBars.entrySet()) {
            Location loc = entry.getKey();
            BossBar bar = entry.getValue();

            boolean shouldSee = player.getLocation().distance(loc) <= VIEW_DISTANCE;
            boolean isSeeing = bar.getPlayers().contains(player);

            if (shouldSee && !isSeeing) {
                bar.addPlayer(player);
            } else if (!shouldSee && isSeeing) {
                bar.removePlayer(player);
            }
        }
    }

    private void initBuildingHP(Block block) {
        ConfigurationSection config = getBuildingConfig(block);
        int maxHP = config.getInt("max-hp", 100);
        Location loc = block.getLocation();
        buildingHP.put(loc, maxHP);
        createHPBar(loc, maxHP);
        originalBlocks.put(loc, block);
    }
    private ConfigurationSection getBuildingConfig(Block block) {
        BuildingData data = plugin.getBuildingData(block.getLocation());
        return plugin.getConfig().getConfigurationSection("buildings." + (data != null ? data.getType() : "townhall"));
    }

    private void createHPBar(Location loc, int maxHP) {
        Block block = loc.getBlock();
        ConfigurationSection hpBarConfig = getBuildingConfig(block).getConfigurationSection("hp-bar");

        String title = ChatColor.translateAlternateColorCodes('&',
                hpBarConfig.getString("title")
                        .replace("{hp}", String.valueOf(maxHP))
                        .replace("{max_hp}", String.valueOf(maxHP)));

        BossBar bar = Bukkit.createBossBar(
                title,
                BarColor.valueOf(hpBarConfig.getString("color")),
                BarStyle.valueOf(hpBarConfig.getString("style"))
        );
        bar.setVisible(hpBarConfig.getBoolean("visible", true));
        bar.setProgress(1.0);
        hpBars.put(loc, bar);
        updateVisiblePlayers(loc);

    }



    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (!buildingHP.containsKey(loc)) return;

        BuildingData data = plugin.getBuildingData(loc);
        if (data == null) return;

        Player player = event.getPlayer();
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        String buildingTeamName = data.getTeam();

        event.setCancelled(true);

        if (player.getGameMode() == GameMode.CREATIVE) {
            destroyBuilding(loc, player);
            return;
        }

        // Проверка команд с защитой от null
        if (buildingTeamName != null && playerTeam != null && playerTeam.getName().equals(buildingTeamName)) {
            player.sendMessage(ChatColor.RED + "Вы не можете ломать здания своей команды!");
            return;
        }

        scheduleDamage(loc, player, calculateDamage(player, player.getInventory().getItemInMainHand()));
    }
    private void scheduleDamage(Location loc, Player player, int damage) {
        if (pendingDamage.containsKey(player)) {
            pendingDamage.get(player).cancel();
        }

        player.sendBlockChange(loc, Material.AIR.createBlockData());
        player.playSound(loc, Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);

        pendingDamage.put(player, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.sendBlockChange(loc, originalBlocks.get(loc).getBlockData());
            applyDamage(loc, player, damage);
            pendingDamage.remove(player);
        }, 20L));
    }
    private void applyDamage(Location loc, Player player, int damage) {
        int currentHP = buildingHP.getOrDefault(loc, 0);
        int newHP = Math.max(0, currentHP - damage);
        buildingHP.put(loc, newHP);

        updateHPBar(loc, newHP);

        String sound = getBuildingConfig(loc.getBlock()).getString("break-sound");
        player.playSound(loc, Sound.valueOf(sound), 1.0f, 1.0f);

        if (newHP <= 0) {
            destroyBuilding(loc, player);
        }
    }
    private int calculateDamage(Player player, ItemStack tool) {
        // Логика расчета урона от инструмента
        if (tool == null) return 1;

        switch (tool.getType()) {
            case WOODEN_PICKAXE: return 2;
            case STONE_PICKAXE: return 3;
            case IRON_PICKAXE: return 5;
            case DIAMOND_PICKAXE: return 7;
            case NETHERITE_PICKAXE: return 9;
            default: return 1;
        }
    }

    private void destroyBuilding(Location loc, Player player) {
        BuildingData data = plugin.getBuildingData(loc);
        if (data != null) {
            player.sendMessage(ChatColor.GREEN + "Вы разрушили здание команды " +
                    plugin.getTeamDisplayName(data.getTeam()));
            plugin.removeBuildingData(loc);
        }

        Block original = originalBlocks.get(loc);
        if (original != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendBlockChange(loc, original.getBlockData());
            }
        }

        if (hpBars.containsKey(loc)) {
            hpBars.get(loc).removeAll();
            hpBars.remove(loc);
        }
        buildingHP.remove(loc);
        originalBlocks.remove(loc);

        loc.getWorld().playEffect(loc, Effect.SMOKE, 10);
        String sound = getBuildingConfig(loc.getBlock()).getString("destroy-sound");
        loc.getWorld().playSound(loc, Sound.valueOf(sound), 1.0f, 1.0f);
    }

    private void updateHPBar(Location loc, int currentHP) {
        BossBar bar = hpBars.get(loc);
        if (bar == null) return;

        Block block = loc.getBlock();
        ConfigurationSection hpBarConfig = getBuildingConfig(block).getConfigurationSection("hp-bar");
        int maxHP = getBuildingConfig(block).getInt("max-hp", 100);
        float progress = (float) currentHP / maxHP;

        String title = ChatColor.translateAlternateColorCodes('&',
                hpBarConfig.getString("title")
                        .replace("{hp}", String.valueOf(currentHP))
                        .replace("{max_hp}", String.valueOf(maxHP)));

        bar.setTitle(title);
        bar.setProgress(progress);
        updateVisiblePlayers(loc);
    }
    private void updateVisiblePlayers(Location loc) {
        BossBar bar = hpBars.get(loc);
        if (bar == null) return;

        new ArrayList<>(bar.getPlayers()).forEach(bar::removePlayer);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getLocation().distance(loc) <= VIEW_DISTANCE) {
                bar.addPlayer(player);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.END_STONE) return;

        BuildingData data = plugin.getBuildingData(block.getLocation());
        if (data == null) return;

        Player player = event.getPlayer();
        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        String buildingTeamName = data.getTeam();

        // Проверка команд с защитой от null
        if (buildingTeamName != null && playerTeam != null && !playerTeam.getName().equals(buildingTeamName)) {
            player.sendMessage(ChatColor.RED + "Это здание принадлежит команде " +
                    plugin.getTeamDisplayName(buildingTeamName));
            event.setCancelled(true);
            return;
        }


        MenuHolder menu = new MenuHolder(plugin, block.getLocation());
        menu.open(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (pendingDamage.containsKey(event.getPlayer())) {
            pendingDamage.get(event.getPlayer()).cancel();
            pendingDamage.remove(event.getPlayer());
        }
        hpBars.values().forEach(bar -> bar.removePlayer(event.getPlayer()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (breakingPlayers.containsKey(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                cancelBreaking(player);
            }
        }
        if (event.getFrom().distanceSquared(event.getTo()) > 9) {
            Location to = event.getTo();
            for (Location loc : hpBars.keySet()) {
                BossBar bar = hpBars.get(loc);
                Player p = event.getPlayer();

                if (to.distance(loc) <= 15) {
                    if (!bar.getPlayers().contains(p)) {
                        bar.addPlayer(p);
                    }
                } else {
                    bar.removePlayer(p);
                }
            }
        }
    }

    private double calculateBreakTime(Player player, ItemStack tool) {
        double baseTime = plugin.getConfig().getDouble("block-breaking.base-break-time", 3.0);
        double multiplier = 1.0;

        if (tool != null && tool.getType().toString().endsWith("_PICKAXE")) {
            multiplier = plugin.getConfig().getDouble("block-breaking.tool-multipliers." + tool.getType(), 1.0);
        }

        if (player.hasPotionEffect(PotionEffectType.HASTE)) {
            multiplier *= 0.3;
        }

        return baseTime * multiplier;
    }

    private void startBreaking(final Player player, final Block block, double breakTime) {
        cancelBreaking(player);

        int breakTicks = (int) (breakTime * 20);
        BlockBreakData data = new BlockBreakData(block, breakTicks);
        breakingPlayers.put(player, data);

        BossBar bossBar = createBossBar(player, block);


        data.task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            data.progress++;
            float progressPercent = (float) data.progress / data.totalTicks;
            float remainingPercent = 1.0f - progressPercent;

            bossBar.setProgress(remainingPercent);
            bossBar.setTitle(String.format("%sПрочность: %.0f%%",
                    getColorByPercent(remainingPercent),
                    remainingPercent * 100));

            player.sendBlockDamage(block.getLocation(), progressPercent);

            if (data.progress >= data.totalTicks) {
                completeBreaking(player, block);
            }
        }, 1L, 1L);
    }

    private BossBar createBossBar(Player player, Block block) {
        ConfigurationSection bossBarConfig = plugin.getConfig().getConfigurationSection("block-breaking.boss-bar");
        String title = ChatColor.translateAlternateColorCodes('&',
                        bossBarConfig.getString("title", "&cПрочность здания"))
                .replace("{percent}", "100");

        BarColor color = BarColor.valueOf(bossBarConfig.getString("color", "RED"));
        BarStyle style = BarStyle.valueOf(bossBarConfig.getString("style", "SEGMENTED_20"));

        BossBar bossBar = Bukkit.createBossBar(title, color, style);
        bossBar.setVisible(true);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        return bossBar;
    }

    private ChatColor getColorByPercent(float percent) {
        if (percent > 0.7) return ChatColor.GREEN;
        if (percent > 0.4) return ChatColor.YELLOW;
        if (percent > 0.2) return ChatColor.GOLD;
        return ChatColor.RED;
    }

    private void completeBreaking(Player player, Block block) {
        BlockBreakData data = breakingPlayers.remove(player);
        if (data != null && data.task != null) {
            data.task.cancel();
        }



        BuildingData buildingData = plugin.getBuildingData(block.getLocation());
        if (buildingData != null) {
            plugin.removeBuildingData(block.getLocation());
            block.setType(Material.AIR);
            player.sendMessage(ChatColor.GREEN + "Вы разрушили здание команды " +
                    plugin.getTeamDisplayName(buildingData.getTeam()));
        }
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
    private void cancelBreaking(Player player) {
        BlockBreakData data = breakingPlayers.remove(player);
        if (data != null) {
            if (data.task != null) {
                data.task.cancel();
            }
            player.sendBlockDamage(data.block.getLocation(), 0.0f);

        }
    }


    public void disable() {
        // Полная очистка
        pendingDamage.values().forEach(BukkitTask::cancel);
        hpBars.values().forEach(BossBar::removeAll);
        hpBars.clear();
        buildingHP.clear();
        originalBlocks.clear();
    }
    private static class BlockBreakData {
        final Block block;
        final int totalTicks;
        int progress = 0;
        BukkitTask task;

        BlockBreakData(Block block, int totalTicks) {
            this.block = block;
            this.totalTicks = totalTicks;
        }
    }


}