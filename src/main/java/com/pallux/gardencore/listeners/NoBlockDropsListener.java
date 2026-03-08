package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class NoBlockDropsListener implements Listener {

    private final GardenCore plugin;

    public NoBlockDropsListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("gc.bypass.noblockdrops")) return;
        if (!plugin.getCropManager().isCrop(event.getBlock().getType())) return;
        event.setDropItems(false);
    }
}
