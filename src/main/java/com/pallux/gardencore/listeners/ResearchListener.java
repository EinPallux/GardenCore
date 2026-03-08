package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.gui.ResearchGui;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class ResearchListener implements Listener {

    private final GardenCore plugin;

    public ResearchListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory inv = event.getInventory();
        if (inv.getSize() != 54) return;

        String guiTitle = ColorUtil.translate(
                plugin.getConfigManager().getResearchConfig()
                        .getString("research.gui-title", "&8Research Menu"));

        if (!event.getView().getTitle().equals(guiTitle)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ResearchGui gui = plugin.getResearchGui();

        if (slot == gui.getSlotCancel()) {
            plugin.getResearchManager().cancelResearch(player);
            reopenGui(player);
            return;
        }

        if (slot == gui.getSlotPrev()) {
            int page = plugin.getPlayerResearchPage(player);
            if (page > 0) plugin.setPlayerResearchPage(player, page - 1);
            reopenGui(player);
            return;
        }

        if (slot == gui.getSlotNext()) {
            int page = plugin.getPlayerResearchPage(player);
            plugin.setPlayerResearchPage(player, page + 1);
            reopenGui(player);
            return;
        }

        int[] slots = gui.getResearchSlots();
        int slotPos = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) { slotPos = i; break; }
        }
        if (slotPos < 0) return;

        int page = plugin.getPlayerResearchPage(player);
        int researchIndex = page * slots.length + slotPos;

        if (researchIndex >= plugin.getResearchManager().getTotalResearches()) return;

        plugin.getResearchManager().startResearch(player, researchIndex);
        reopenGui(player);
    }

    private void reopenGui(Player player) {
        plugin.getResearchGui().open(player, plugin.getPlayerResearchPage(player));
    }
}