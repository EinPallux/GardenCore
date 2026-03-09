package com.pallux.gardencore.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Marker holder for the Research GUI inventory.
 * Using a custom InventoryHolder allows reliable GUI detection without
 * fragile title-string comparisons that can break across server versions.
 */
public class ResearchHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}