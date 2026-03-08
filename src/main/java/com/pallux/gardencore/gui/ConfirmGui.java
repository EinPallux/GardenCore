package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

public class ConfirmGui implements Listener {

    private final GardenCore plugin;
    private final Player sender;
    private final Inventory inventory;
    private final Runnable onConfirm;

    public ConfirmGui(GardenCore plugin, Player sender, String title, Runnable onConfirm) {
        this.plugin = plugin;
        this.sender = sender;
        this.onConfirm = onConfirm;
        this.inventory = Bukkit.createInventory(null, 27, ColorUtil.translate(title));
        build();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        var filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inventory.setItem(i, filler);

        var confirm = GuiUtil.createItem(Material.GREEN_STAINED_GLASS_PANE,
                "&a&lConfirm",
                plugin.getConfigManager().getMessage("admin.reset-confirm-lore")
        );
        var cancel = GuiUtil.createItem(Material.RED_STAINED_GLASS_PANE,
                "&c&lCancel",
                plugin.getConfigManager().getMessage("admin.reset-cancel-lore")
        );

        inventory.setItem(11, confirm);
        inventory.setItem(15, cancel);
    }

    public void open() {
        sender.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(sender)) return;
        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 11) {
            sender.closeInventory();
            onConfirm.run();
        } else if (slot == 15) {
            sender.closeInventory();
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}
