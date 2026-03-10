package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.PetsGui;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PetsCommand implements CommandExecutor {

    private final GardenCore plugin;

    public PetsCommand(GardenCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "command-player-only");
            return true;
        }

        if (!player.hasPermission("gc.player")) {
            MessageUtil.send(sender, "no-permission");
            return true;
        }

        new PetsGui(plugin, player).open();
        return true;
    }
}