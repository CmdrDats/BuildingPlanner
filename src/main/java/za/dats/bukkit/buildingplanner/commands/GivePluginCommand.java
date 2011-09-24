package za.dats.bukkit.buildingplanner.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import za.dats.bukkit.buildingplanner.BuildingPlanner;

public class GivePluginCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("Hello world"+sender.getClass());
        if (args.length > 0) {
            Player player = (Player) sender;
            
            if ("clear".equals(args[0])) {
        	ItemStack[] contents = player.getInventory().getContents();
        	for (int i = 0; i < contents.length; i++) {
        	    player.getInventory().setItem(i, null);
        	}
            }
        }
        return true;
    }
}
