package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.ElderGui;
import com.pallux.gardencore.gui.ElderHolder;
import com.pallux.gardencore.managers.ElderManager;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

import java.util.Map;

public class ElderListener implements Listener {

    private final GardenCore plugin;

    public ElderListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (!(inv.getHolder() instanceof ElderHolder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 36) return;

        ElderGui gui = plugin.getElderGui();

        if (slot == gui.getSlotClose()) {
            player.closeInventory();
            return;
        }

        ElderManager.ElderPerkType type = resolveType(slot, gui);
        if (type == null) return;

        ElderManager.PurchaseResult result = plugin.getElderManager().tryPurchase(player, type);

        switch (result) {
            case MAX_REACHED -> MessageUtil.send(player, "elder.max-reached",
                    Map.of("perk", plugin.getElderManager().getDisplayName(type)));
            case NOT_ENOUGH_FIBER ->
                    MessageUtil.send(player, "elder.not-enough-fiber");
            case NOT_ENOUGH_DRIFTWOOD ->
                    MessageUtil.send(player, "elder.not-enough-driftwood");
            case NOT_ENOUGH_MOSS ->
                    MessageUtil.send(player, "elder.not-enough-moss");
            case NOT_ENOUGH_REED ->
                    MessageUtil.send(player, "elder.not-enough-reed");
            case NOT_ENOUGH_CLOVER ->
                    MessageUtil.send(player, "elder.not-enough-clover");
            case SUCCESS -> {
                // Refresh GUI so updated levels/costs are shown immediately
                plugin.getElderGui().open(player);
            }
        }
    }

    private ElderManager.ElderPerkType resolveType(int slot, ElderGui gui) {
        if (slot == gui.getSlotFiber())    return ElderManager.ElderPerkType.FIBER_AMOUNT;
        if (slot == gui.getSlotMaterial()) return ElderManager.ElderPerkType.MATERIAL_AMOUNT;
        if (slot == gui.getSlotXp())       return ElderManager.ElderPerkType.XP_GAIN;
        if (slot == gui.getSlotChance())   return ElderManager.ElderPerkType.MATERIAL_CHANCE;
        return null;
    }
}