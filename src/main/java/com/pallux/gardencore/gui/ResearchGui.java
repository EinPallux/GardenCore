package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class ResearchGui {

    // Slots in the 6-row (54-slot) GUI where researches are displayed
    // Rows 1-4, columns 1-7 (inner 7 slots per row, skipping edge cols 0 and 8)
    private static final int[] RESEARCH_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,  // row 1
            19, 20, 21, 22, 23, 24, 25,  // row 2
            28, 29, 30, 31, 32, 33, 34,  // row 3
            37, 38, 39, 40, 41, 42, 43   // row 4
    };

    // Bottom bar navigation slots
    private static final int SLOT_PREV = 45;
    private static final int SLOT_CANCEL = 49;
    private static final int SLOT_NEXT = 53;

    private final GardenCore plugin;

    public ResearchGui(GardenCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        FileConfiguration cfg = plugin.getConfigManager().getResearchConfig();
        String title = ColorUtil.translate(cfg.getString("research.gui-title", "&8Research Menu"));

        Inventory inv = Bukkit.createInventory(null, 54, title);
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        int total = plugin.getResearchManager().getTotalResearches();
        int perPage = RESEARCH_SLOTS.length; // 28 per page
        int maxPage = Math.max(0, (total - 1) / perPage);
        page = Math.max(0, Math.min(page, maxPage));

        // Fill all slots with gray glass first
        ItemStack filler = GuiUtil.createFiller(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Place research items
        int startIndex = page * perPage;
        for (int i = 0; i < RESEARCH_SLOTS.length; i++) {
            int researchIndex = startIndex + i;
            if (researchIndex >= total) break;
            inv.setItem(RESEARCH_SLOTS[i], buildResearchItem(researchIndex, data, cfg));
        }

        // Bottom bar: prev page
        if (page > 0) {
            inv.setItem(SLOT_PREV, GuiUtil.createItem(Material.STICK,
                    "&#FFD700◀ Previous Page",
                    "&7Page " + page + " / " + (maxPage + 1)));
        } else {
            inv.setItem(SLOT_PREV, filler);
        }

        // Bottom bar: cancel / info slot
        if (data.hasActiveResearch()) {
            List<String> cancelLore = cfg.getStringList("research.cancel-lore")
                    .stream().map(ColorUtil::translate).collect(Collectors.toList());
            inv.setItem(SLOT_CANCEL, GuiUtil.createItem(Material.BARRIER,
                    cfg.getString("research.cancel-name", "&#ff7a7a&lCancel Research"),
                    cancelLore));
        } else {
            inv.setItem(SLOT_CANCEL, GuiUtil.createItem(Material.GRAY_STAINED_GLASS_PANE,
                    cfg.getString("research.no-research-name", "&7No Research Active"),
                    cfg.getStringList("research.no-research-lore")
                            .stream().map(ColorUtil::translate).collect(Collectors.toList())));
        }

        // Bottom bar: next page
        if (page < maxPage) {
            inv.setItem(SLOT_NEXT, GuiUtil.createItem(Material.STICK,
                    "&#FFD700Next Page ▶",
                    "&7Page " + (page + 2) + " / " + (maxPage + 1)));
        } else {
            inv.setItem(SLOT_NEXT, filler);
        }

        player.openInventory(inv);
    }

    private ItemStack buildResearchItem(int index, PlayerData data, FileConfiguration cfg) {
        String name = plugin.getResearchManager().getResearchName(index);
        double fiberPerResearch = cfg.getDouble("research.fiber-amount-per-research", 0.1);
        double materialPerResearch = cfg.getDouble("research.material-amount-per-research", 0.1);

        // The bonus THIS research unlocks (its own level, not cumulative display)
        double thisFiber = (index + 1) * fiberPerResearch;
        double thisMaterial = (index + 1) * materialPerResearch;

        String fiberStr = String.format("%.1f", thisFiber);
        String materialStr = String.format("%.1f", thisMaterial);

        long durationSec = plugin.getResearchManager().getDurationMs(index) / 1000;
        String timeStr = plugin.getResearchManager().formatDuration(durationSec);
        double cost = plugin.getResearchManager().getCost(index);
        String costStr = com.pallux.gardencore.utils.NumberUtil.formatRaw(cost);

        int completed = data.getCompletedResearches();
        boolean isCompleted = index < completed;
        boolean isActive = data.hasActiveResearch() && data.getActiveResearchIndex() == index;
        boolean isReady = !isCompleted && !isActive && index == completed;
        // locked: index > completed and not active

        if (isCompleted) {
            String displayName = cfg.getString("research.name-completed", "&#a8ff78&l{name} &7[Completed]")
                    .replace("{name}", name);
            List<String> lore = applyLorePlaceholders(
                    cfg.getStringList("research.lore-completed"),
                    fiberStr, materialStr, timeStr, "", costStr);
            return GuiUtil.createItem(Material.LIME_STAINED_GLASS_PANE, displayName, lore);
        }

        if (isActive) {
            long remainMs = plugin.getResearchManager().getTimeRemainingMs(data);
            String remaining = plugin.getResearchManager().formatDuration(remainMs / 1000);
            String displayName = cfg.getString("research.name-in-progress", "&#FFD700&l{name} &e[In Progress]")
                    .replace("{name}", name);
            List<String> lore = applyLorePlaceholders(
                    cfg.getStringList("research.lore-in-progress"),
                    fiberStr, materialStr, timeStr, remaining, costStr);
            return GuiUtil.createItem(Material.YELLOW_STAINED_GLASS_PANE, displayName, lore);
        }

        if (isReady) {
            String displayName = cfg.getString("research.name-ready", "&#FFD700&l{name} &e[Click to Research]")
                    .replace("{name}", name);
            List<String> lore = applyLorePlaceholders(
                    cfg.getStringList("research.lore-ready"),
                    fiberStr, materialStr, timeStr, "", costStr);
            return GuiUtil.createItem(Material.YELLOW_STAINED_GLASS_PANE, displayName, lore);
        }

        // Locked
        String displayName = cfg.getString("research.name-locked", "&#ff7a7a&l{name} &7[Locked]")
                .replace("{name}", name);
        List<String> lore = applyLorePlaceholders(
                cfg.getStringList("research.lore-locked"),
                fiberStr, materialStr, timeStr, "", costStr);
        return GuiUtil.createItem(Material.RED_STAINED_GLASS_PANE, displayName, lore);
    }

    private List<String> applyLorePlaceholders(List<String> lines,
                                               String fiberStr, String materialStr,
                                               String timeStr, String remaining, String costStr) {
        return lines.stream()
                .map(l -> l
                        .replace("{fiber_multi}", fiberStr)
                        .replace("{material_multi}", materialStr)
                        .replace("{time}", timeStr)
                        .replace("{time_remaining}", remaining)
                        .replace("{cost}", costStr))
                .map(ColorUtil::translate)
                .collect(Collectors.toList());
    }

    public int[] getResearchSlots() { return RESEARCH_SLOTS; }
    public int getSlotCancel() { return SLOT_CANCEL; }
    public int getSlotPrev() { return SLOT_PREV; }
    public int getSlotNext() { return SLOT_NEXT; }
}