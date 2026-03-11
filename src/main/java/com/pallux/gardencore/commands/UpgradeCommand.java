package com.pallux.gardencore.commands;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.UpgradeGui;
import com.pallux.gardencore.managers.UpgradeManager;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class UpgradeCommand implements CommandExecutor, TabCompleter {

    private final GardenCore plugin;

    public UpgradeCommand(GardenCore plugin) {
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

        if (args.length == 0) {
            new UpgradeGui(plugin, player).open();
            return true;
        }

        UpgradeManager.UpgradeType type = UpgradeManager.fromString(args[0]);
        if (type == null) {
            MessageUtil.send(player, "invalid-arguments", Map.of("usage", "/" + label + " [fiber_amount|material_amount|material_chance]"));
            return true;
        }

        UpgradeManager.UpgradeResult result = plugin.getUpgradeManager().tryUpgrade(player, type);

        switch (result) {
            case MAX_REACHED -> MessageUtil.send(player, "upgrades.max-reached");
            case NOT_ENOUGH_FIBER -> MessageUtil.send(player, "upgrades.not-enough-fiber");
            case SUCCESS -> plugin.getDataManager().savePlayerAsync(player.getUniqueId());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return List.of("fiber_amount", "material_amount", "material_chance", "crop_cooldown").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}