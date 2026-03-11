package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockedCommandListener implements Listener {

    private final GardenCore plugin;
    private final Set<String> blockedCommands = new HashSet<>();

    public BlockedCommandListener(GardenCore plugin) {
        this.plugin = plugin;
        loadBlockedCommands();
    }

    private void loadBlockedCommands() {
        blockedCommands.clear();
        List<String> list = plugin.getConfigManager().getBlockedCommands().getStringList("blocked-commands");
        for (String cmd : list) {
            if (cmd != null && !cmd.isBlank()) {
                blockedCommands.add(cmd.toLowerCase().trim());
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Bypass for admins
        if (player.hasPermission("gc.admin")) return;

        // Extract the base command label (strip leading slash, drop arguments)
        String raw = event.getMessage().substring(1); // remove "/"
        String label = raw.split(" ")[0].toLowerCase();

        // Also derive the un-namespaced label (e.g. "bukkit:plugins" → "plugins")
        String unNamespaced = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;

        if (blockedCommands.contains(label) || blockedCommands.contains(unNamespaced)) {
            event.setCancelled(true);
            MessageUtil.send(player, "command-blocked");
        }
    }
}