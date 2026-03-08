package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class AliasCommand implements CommandExecutor, TabCompleter {

    private final GardenCore plugin;
    private final String targetCommand;

    public AliasCommand(GardenCore plugin, String targetCommand) {
        this.plugin = plugin;
        this.targetCommand = targetCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use alias commands.");
            return true;
        }

        String full = targetCommand;
        if (args.length > 0) {
            full += " " + String.join(" ", args);
        }

        plugin.getServer().dispatchCommand(player, full);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }
}
