package org.showlee.townSystem;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GiveRatushaCommand implements CommandExecutor {
    private final TownSystem plugin;

    public GiveRatushaCommand(TownSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Эта команда только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        // Проверка прав
        if (!player.hasPermission("townsystem.giveratusha")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на эту команду!");
            return true;
        }

        // Создание блока Ратуши
        ItemStack ratushaItem = new ItemStack(Material.STRUCTURE_BLOCK);
        ItemMeta meta = ratushaItem.getItemMeta();

        // Установка названия из конфига
        meta.setDisplayName(plugin.getBuildingItemName());
        ratushaItem.setItemMeta(meta);

        // Выдача предмета игроку
        player.getInventory().addItem(ratushaItem);
        player.sendMessage(ChatColor.GREEN + "Вы получили блок Ратуши!");

        return true;
    }
}