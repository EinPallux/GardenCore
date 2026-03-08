package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;

public class NoDropListener implements Listener {

    private final GardenCore plugin;

    public NoDropListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().hasPermission("gc.bypass.nodrop")) return;
        event.setCancelled(true);
    }
}
