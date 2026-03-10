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
        plugin.getEventManager().addPlayer(event.getPlayer());

        // Spawn pet cosmetic a tick later so player data is fully loaded first
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getPetCosmeticManager().refresh(event.getPlayer()), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().removePlayer(event.getPlayer());
        plugin.getPetCosmeticManager().despawn(event.getPlayer().getUniqueId());
        plugin.getDataManager().removePlayerData(event.getPlayer().getUniqueId());
    }
}