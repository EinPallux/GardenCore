package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
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

        event.setCancelled(true);
        plugin.getItemManager().executeItem(event.getPlayer(), item);
    }
}