package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class InstantReplantListener implements Listener {

    private final GardenCore plugin;

    public InstantReplantListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("gc.bypass.instantreplant")) return;

        Block block = event.getBlock();
        Material type = block.getType();

        if (!plugin.getCropManager().isCrop(type)) return;

        long delayTicks = Math.round(plugin.getConfigManager().getCrops().getDouble("crop-cooldown", 0.1) * 20);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> block.setType(type), Math.max(1, delayTicks));
    }
}