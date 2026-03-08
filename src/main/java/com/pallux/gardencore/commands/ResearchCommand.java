package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResearchCommand implements CommandExecutor {

    private final GardenCore plugin;

    public ResearchCommand(GardenCore plugin) {
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

        // Check if any in-progress research has completed before opening
        plugin.getResearchManager().checkCompletion(player);

        int page = plugin.getPlayerResearchPage(player);
        plugin.getResearchGui().open(player, page);
        return true;
    }
}