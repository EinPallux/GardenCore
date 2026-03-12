package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final GardenCore plugin;

    public PlayerConnectionListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    // 1. Load data completely off the main thread before the player spawns
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        // Only load if the server is going to allow them to join (prevents memory leaks from banned players)
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.getDataManager().loadPlayer(event.getUniqueId());
        }
    }

    // 2. Setup the player on the main thread (Zero disk I/O delays here!)
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Fallback: If another plugin bypassed the pre-login event, load them synchronously just to be safe
        if (!plugin.getDataManager().hasPlayerData(event.getPlayer().getUniqueId())) {
            plugin.getDataManager().loadPlayer(event.getPlayer().getUniqueId());
        }

        plugin.getEventManager().addPlayer(event.getPlayer());
        plugin.getBossManager().addPlayer(event.getPlayer());

        // Spawn pet cosmetic a few ticks later so data is fully loaded first.
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> plugin.getPetCosmeticManager().refresh(event.getPlayer()), 5L);
    }

    // 3. Safely save their file off the main thread and remove them from memory
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEventManager().removePlayer(event.getPlayer());
        plugin.getBossManager().removePlayer(event.getPlayer());
        plugin.getPetCosmeticManager().despawn(event.getPlayer().getUniqueId());

        // Correctly save and remove their data instance asynchronously
        plugin.getDataManager().unloadPlayer(event.getPlayer().getUniqueId());
    }
}