package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.ComposterData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CustomItemListener implements Listener {

    private final GardenCore plugin;

    public CustomItemListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (!plugin.getItemManager().isCustomItem(item)) return;

        // Composter items are handled entirely by ComposterListener on block place.
        // They must not be consumed or executed here on right-click.
        String key = plugin.getItemManager().getItemKey(item);
        if (key != null && ComposterData.ComposterType.fromItemKey(key) != null) return;

        event.setCancelled(true);
        plugin.getItemManager().executeItem(event.getPlayer(), item);
    }
}