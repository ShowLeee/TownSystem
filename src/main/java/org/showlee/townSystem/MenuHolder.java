package org.showlee.townSystem;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class MenuHolder implements InventoryHolder {
    private final Inventory inventory;
    private final Location location;
    private final TownSystem plugin;
    private final BuildingData buildingData;

    public MenuHolder(TownSystem plugin, Location location) {
        this.plugin = plugin;
        this.location = location;
        this.buildingData = plugin.getBuildingData(location);
        this.inventory = Bukkit.createInventory(this, 27, ChatColor.BLUE + "Меню Ратуши");
        setupItems();
    }

    private void setupItems() {
        // Границы меню
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, border);
            inventory.setItem(i + 18, border);
        }
        inventory.setItem(9, border);
        inventory.setItem(17, border);

        // Информация о команде
        ItemStack teamInfo = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta teamMeta = teamInfo.getItemMeta();
        teamMeta.setDisplayName(ChatColor.GOLD + "Команда: " + plugin.getTeamDisplayName(buildingData.getTeam()));
        teamMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Уровень: " + ChatColor.YELLOW + buildingData.getLevel(),
                "",
                ChatColor.DARK_GRAY + "ID: " + buildingData.getType()
        ));
        teamInfo.setItemMeta(teamMeta);
        inventory.setItem(4, teamInfo);

        // Кнопка сдачи ресурсов
        ItemStack submitBtn = new ItemStack(Material.HOPPER);
        ItemMeta submitMeta = submitBtn.getItemMeta();
        submitMeta.setDisplayName(ChatColor.YELLOW + "Сдать ресурсы");
        submitMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Нажмите, чтобы сдать ресурсы",
                ChatColor.GRAY + "для улучшения здания",
                "",
                ChatColor.DARK_GRAY + "[ЛКМ] - Сдать ресурсы"
        ));
        submitBtn.setItemMeta(submitMeta);
        inventory.setItem(13, submitBtn);

        // Кнопка информации
        ItemStack infoBtn = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = infoBtn.getItemMeta();
        infoMeta.setDisplayName(ChatColor.AQUA + "Информация");
        infoMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Требуемые ресурсы:",
                ChatColor.WHITE + "- Камень: 64",
                ChatColor.WHITE + "- Дерево: 32",
                ChatColor.WHITE + "- Железо: 16",
                "",
                ChatColor.DARK_GRAY + "Уровень " + buildingData.getLevel()
        ));
        infoBtn.setItemMeta(infoMeta);
        inventory.setItem(11, infoBtn);

        // Кнопка наград
        ItemStack rewardsBtn = new ItemStack(Material.CHEST);
        ItemMeta rewardsMeta = rewardsBtn.getItemMeta();
        rewardsMeta.setDisplayName(ChatColor.GREEN + "Награды");
        rewardsMeta.setLore(Arrays.asList(
                "",
                ChatColor.GRAY + "Награды за улучшение:",
                ChatColor.WHITE + "- Изумруды: 5",
                ChatColor.WHITE + "- Блок шахты: 1",
                "",
                ChatColor.DARK_GRAY + "Следующий уровень: " + (buildingData.getLevel() + 1)
        ));
        rewardsBtn.setItemMeta(rewardsMeta);
        inventory.setItem(15, rewardsBtn);

        // Кнопка закрытия
        ItemStack closeBtn = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeBtn.getItemMeta();
        closeMeta.setDisplayName(ChatColor.RED + "Закрыть");
        closeBtn.setItemMeta(closeMeta);
        inventory.setItem(22, closeBtn);
    }

    public void open(Player player) {
        if (buildingData == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: данные здания не найдены!");
            return;
        }

        Team playerTeam = player.getScoreboard().getPlayerTeam(player);
        Team buildingTeam = player.getScoreboard().getTeam(buildingData.getTeam());

        if (buildingTeam != null && !buildingTeam.equals(playerTeam)) {
            player.sendMessage(ChatColor.RED + "Вы не можете взаимодействовать с зданиями чужой команды!");
            return;
        }

        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();

        switch (slot) {
            case 13: // Кнопка сдачи ресурсов
                handleResourceSubmit(player);
                break;
            case 22: // Кнопка закрытия
                player.closeInventory();
                break;
        }
    }

    private void handleResourceSubmit(Player player) {
        // Проверяем ресурсы в инвентаре
        boolean hasResources = checkResources(player);

        if (!hasResources) {
            player.sendMessage(ChatColor.RED + "У вас недостаточно ресурсов для улучшения!");
            return;
        }

        // Забираем ресурсы
        takeResources(player);

        // Улучшаем здание
        buildingData.setLevel(buildingData.getLevel() + 1);
        plugin.updateBuildingData(buildingData);

        // Выдаем награду
        giveRewards(player);

        player.sendMessage(ChatColor.GREEN + "Ратуша улучшена до уровня " + buildingData.getLevel() + "!");
        player.closeInventory();
    }

    private boolean checkResources(Player player) {
        // Здесь должна быть логика проверки ресурсов
        // Временная заглушка - всегда возвращает true для теста
        return true;
    }

    private void takeResources(Player player) {
        // Здесь должна быть логика изъятия ресурсов
    }

    private void giveRewards(Player player) {
        // Здесь должна быть логика выдачи наград
        player.getInventory().addItem(new ItemStack(Material.EMERALD, 5));
        player.sendMessage(ChatColor.GREEN + "Вы получили награду за улучшение!");
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}