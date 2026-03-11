package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.ComposterData;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ComposterListener implements Listener {

    private final GardenCore plugin;

    public ComposterListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItemInHand();

        if (event.getBlock().getType() != Material.COMPOSTER) return;

        String itemKey = plugin.getItemManager().getItemKey(item);
        if (itemKey == null) return;

        ComposterData.ComposterType type = ComposterData.ComposterType.fromItemKey(itemKey);
        if (type == null) return;

        // Read duration and radius from the item definition; fall back to sensible defaults
        int    duration = plugin.getItemManager().getDurationSeconds(itemKey);
        double radius   = plugin.getItemManager().getRadius(itemKey);
        if (duration <= 0) duration = 900;
        if (radius   <= 0) radius   = 20.0;

        // Consume the item
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin,
                    () -> player.getInventory().removeItem(item), 1L);
        }

        final int    finalDuration = duration;
        final double finalRadius   = radius;
        Block block = event.getBlock();

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getComposterManager().placeComposter(
                    player, block.getLocation(), type, finalDuration, finalRadius);

            MessageUtil.send(player, "composter.placed", Map.of(
                    "type",     type.getDisplayName(),
                    "duration", String.valueOf(finalDuration / 60),
                    "radius",   String.valueOf((int) finalRadius)
            ));
        }, 1L);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.COMPOSTER) return;
        if (!plugin.getComposterManager().isComposter(block.getLocation())) return;

        event.setDropItems(false);
        plugin.getComposterManager().removeComposter(block.getLocation());
        MessageUtil.send(event.getPlayer(), "composter.removed");
    }
}