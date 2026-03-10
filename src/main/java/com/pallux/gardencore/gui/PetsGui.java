package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.managers.PetManager;
import com.pallux.gardencore.models.PetRarity;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class PetsGui implements Listener {

    private final GardenCore plugin;
    private final Player player;
    private final Inventory inventory;

    private final int size;
    private final int slotPet;
    private final int slotOverview;

    public PetsGui(GardenCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration cfg = plugin.getConfigManager().getPetsConfig();

        int rawSize = cfg.getInt("pets.gui-size", 18);
        this.size = Math.min(54, Math.max(9, (int) Math.ceil(rawSize / 9.0) * 9));

        this.slotPet      = clampSlot(cfg.getInt("pets.pet-slot",      10));
        this.slotOverview = clampSlot(cfg.getInt("pets.overview-slot", 15));

        String title = cfg.getString("pets.gui-title", "Pets Menu");
        this.inventory = Bukkit.createInventory(null, this.size, ColorUtil.translate(title));

        build();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // ── Build ──────────────────────────────────────────────────

    private void build() {
        FileConfiguration cfg = plugin.getConfigManager().getPetsConfig();
        Material fillerMat = parseMaterial(cfg.getString("pets.filler.material"), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = GuiUtil.createFiller(fillerMat);
        for (int i = 0; i < size; i++) inventory.setItem(i, filler);

        inventory.setItem(slotPet,      buildPetItem());
        inventory.setItem(slotOverview, buildOverviewItem());
    }

    private ItemStack buildPetItem() {
        PetManager pm     = plugin.getPetManager();
        PetRarity rarity  = pm.getPlayerPet(player.getUniqueId());
        FileConfiguration cfg = plugin.getConfigManager().getPetsConfig();

        if (rarity == PetRarity.NONE) {
            Material mat  = parseMaterial(cfg.getString("pets.no-pet-material"), Material.BARRIER);
            String name   = cfg.getString("pets.no-pet-name", "&7No Pet Found");
            List<String> lore = colorList(cfg.getStringList("pets.no-pet-lore"));
            return GuiUtil.createItem(mat, name, lore);
        }

        String color       = pm.getChatColor(rarity);
        String displayName = pm.getDisplayName(rarity);
        String bonus       = NumberUtil.formatRaw(pm.getFiberBonus(rarity));
        String coloredRarity = color + "&l" + displayName + "&r";

        String name = ColorUtil.translate(
                cfg.getString("pets.pet-name", "{rarity} &7Pet")
                        .replace("{rarity}", coloredRarity)
        );

        List<String> lore = new ArrayList<>();
        for (String line : cfg.getStringList("pets.pet-lore")) {
            lore.add(ColorUtil.translate(line
                    .replace("{rarity}",      coloredRarity)
                    .replace("{fiber_bonus}", bonus)
            ));
        }

        return GuiUtil.createItem(pm.getGlassMaterial(rarity), name, lore);
    }

    private ItemStack buildOverviewItem() {
        PetManager pm = plugin.getPetManager();
        FileConfiguration cfg = plugin.getConfigManager().getPetsConfig();

        Material mat = parseMaterial(cfg.getString("pets.overview-material"), Material.DRAGON_EGG);
        String name  = cfg.getString("pets.overview-name", "&#d4a8ff&l✦ Pet Overview");

        // Split the lore list into static lines and the rarity-line template
        List<String> rawLore = cfg.getStringList("pets.overview-lore");
        String rarityLineTemplate = null;
        List<String> otherLines   = new ArrayList<>();

        for (String line : rawLore) {
            if (line.startsWith("rarity-line: ")) {
                rarityLineTemplate = line.substring("rarity-line: ".length());
            } else {
                otherLines.add(line);
            }
        }

        // Rebuild lore, injecting per-rarity lines after the separator header
        List<String> lore = new ArrayList<>();
        boolean injected = false;
        for (String line : otherLines) {
            lore.add(ColorUtil.translate(line));
            if (!injected && line.contains("───")) {
                injected = true;
                injectRarityLines(lore, pm, rarityLineTemplate);
            }
        }
        if (!injected) injectRarityLines(lore, pm, rarityLineTemplate);

        return GuiUtil.createItem(mat, name, lore);
    }

    private void injectRarityLines(List<String> lore, PetManager pm, String template) {
        if (template == null) template = "{rarity} &8| &71/{chance} &8| &#FFD700+{fiber_bonus}x Fiber";
        for (PetRarity r : PetRarity.values()) {
            if (r == PetRarity.NONE) continue;
            int chance = pm.getOneInChance(r);
            if (chance <= 0) continue;
            lore.add(ColorUtil.translate(template
                    .replace("{rarity}",      pm.getChatColor(r) + pm.getDisplayName(r))
                    .replace("{chance}",      NumberUtil.formatRaw(chance))
                    .replace("{fiber_bonus}", NumberUtil.formatRaw(pm.getFiberBonus(r)))
            ));
        }
    }

    // ── Open ───────────────────────────────────────────────────

    public void open() {
        player.openInventory(inventory);
    }

    // ── Events ─────────────────────────────────────────────────

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }

    // ── Helpers ────────────────────────────────────────────────

    private int clampSlot(int slot) {
        return Math.min(Math.max(0, slot), size - 1);
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : fallback;
    }

    private List<String> colorList(List<String> lines) {
        List<String> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(ColorUtil.translate(l));
        return out;
    }
}