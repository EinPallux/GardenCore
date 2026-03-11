package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final GardenCore plugin;

    public PlayerConnectionListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getDataManager().loadPlayer(event.getPlayer().getUniqueId());

        plugin.getEventManager().addPlayer(event.getPlayer());
        plugin.getBossManager().addPlayer(event.getPlayer());

        // Spawn pet cosmetic a few ticks later so data is fully loaded first.
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getPetCosmeticManager().refresh(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().removePlayer(event.getPlayer());
        plugin.getBossManager().removePlayer(event.getPlayer());
        plugin.getPetCosmeticManager().despawn(event.getPlayer().getUniqueId());

        // Correctly save and remove their data instance asynchronously
        plugin.getDataManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}