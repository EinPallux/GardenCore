package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.managers.ElderManager;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ElderGui {

    private final GardenCore plugin;

    public ElderGui(GardenCore plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        FileConfiguration cfg = plugin.getConfigManager().getElderConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("elder-menu");

        String title = sec != null
                ? ColorUtil.translate(sec.getString("title", "&5&lElder Council"))
                : ColorUtil.translate("&5&lElder Council");
        int size = sec != null ? sec.getInt("size", 36) : 36;

        ElderHolder holder = new ElderHolder();
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.setInventory(inv);

        if (sec != null) buildInventory(inv, player, sec, size);
        player.openInventory(inv);
    }

    private void buildInventory(Inventory inv, Player player, ConfigurationSection sec, int size) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);

        // ── Filler ────────────────────────────────────────────
        Material fillerMat = parseMaterial(sec.getString("filler.material"), Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = GuiUtil.createFiller(fillerMat);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // ── Top-border row (row 0) ─────────────────────────────
        Material topMat = parseMaterial(sec.getString("top-border.material"), Material.PURPLE_STAINED_GLASS_PANE);
        ItemStack topBorder = GuiUtil.createFiller(topMat);
        for (int i = 0; i < 9; i++) inv.setItem(i, topBorder);

        // ── Side + bottom border ───────────────────────────────
        Material borderMat = parseMaterial(sec.getString("border.material"), Material.BLACK_STAINED_GLASS_PANE);
        ItemStack border = GuiUtil.createFiller(borderMat);
        int rows = size / 9;
        // Side columns for middle rows (not top, not bottom)
        for (int row = 1; row < rows - 1; row++) {
            inv.setItem(row * 9,         border);
            inv.setItem(row * 9 + 8,     border);
        }
        // Bottom row
        for (int i = (rows - 1) * 9; i < size; i++) inv.setItem(i, border);

        // ── Header item ────────────────────────────────────────
        ConfigurationSection headerSec = sec.getConfigurationSection("header");
        if (headerSec != null) {
            int headerSlot = headerSec.getInt("slot", 4);
            Material headerMat = parseMaterial(headerSec.getString("material"), Material.TOTEM_OF_UNDYING);
            String headerName = headerSec.getString("name", "&#d4a8ff&l⚗ Elder Council");
            List<String> headerLore = headerSec.getStringList("lore").stream()
                    .map(ColorUtil::translate).toList();
            inv.setItem(headerSlot, GuiUtil.createItem(headerMat, headerName, headerLore));
        }

        // ── Perk cards ─────────────────────────────────────────
        ConfigurationSection perksSec = sec.getConfigurationSection("perks");
        if (perksSec != null) {
            for (ElderManager.ElderPerkType type : ElderManager.ElderPerkType.values()) {
                String key = plugin.getElderManager().configKey(type);
                ConfigurationSection pSec = perksSec.getConfigurationSection(key);
                if (pSec == null) continue;
                int slot = pSec.getInt("slot", defaultSlot(type));
                if (slot >= 0 && slot < size) {
                    inv.setItem(slot, buildPerkItem(type, data, uuid, sec));
                }
            }
        }

        // ── Resource info item ─────────────────────────────────
        ConfigurationSection infoSec = sec.getConfigurationSection("info");
        if (infoSec != null) {
            int infoSlot = infoSec.getInt("slot", 31);
            if (infoSlot >= 0 && infoSlot < size) {
                inv.setItem(infoSlot, buildResourceInfo(data, sec));
            }
        }

        // ── Close button ───────────────────────────────────────
        ConfigurationSection closeSec = sec.getConfigurationSection("close-button");
        if (closeSec != null) {
            int closeSlot = closeSec.getInt("slot", size - 1);
            if (closeSlot >= 0 && closeSlot < size) {
                Material closeMat = parseMaterial(closeSec.getString("material"), Material.BARRIER);
                String closeName = closeSec.getString("name", "&#ff7a7a&lClose");
                List<String> closeLore = closeSec.getStringList("lore").stream()
                        .map(ColorUtil::translate).toList();
                inv.setItem(closeSlot, GuiUtil.createItem(closeMat, closeName, closeLore));
            }
        }
    }

    private ItemStack buildPerkItem(ElderManager.ElderPerkType type, PlayerData data, UUID uuid,
                                    ConfigurationSection sec) {
        ElderManager em = plugin.getElderManager();
        int current   = em.getCurrentLevel(uuid, type);
        int max       = em.getMaxLevel(type);
        boolean maxed = current >= max;

        double fiberCost  = em.getFiberCost(type, current);
        double dwood      = em.getDriftwoodCost(type, current);
        double moss       = em.getMossCost(type, current);
        double reed       = em.getReedCost(type, current);
        double clover     = em.getCloverCost(type, current);
        double totalBonus = em.getTotalBonus(uuid, type) * 100;
        double nextBonus  = em.getBonusPerLevel(type) * 100;

        Material mat = parseMaterial(
                sec.getConfigurationSection("perks." + em.configKey(type)) != null
                        ? sec.getConfigurationSection("perks." + em.configKey(type)).getString("material")
                        : null,
                perkMaterialFallback(type));

        String nameKey = maxed ? "perk-name-maxed" : "perk-name";
        String rawName = (sec.getString(nameKey, maxed ? "&#d4a8ff&l{perk} &#a8ff78[MAX]" : "&#d4a8ff&l{perk}"))
                .replace("{perk}", em.getDisplayName(type));

        List<String> loreTemplate = sec.getStringList(maxed ? "perk-lore-maxed" : "perk-lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ColorUtil.translate(line
                    .replace("{level}",           String.valueOf(current))
                    .replace("{max_level}",        String.valueOf(max))
                    .replace("{bonus}",            NumberUtil.formatRaw(totalBonus))
                    .replace("{next_bonus}",       NumberUtil.formatRaw(nextBonus))
                    .replace("{fiber_cost}",       maxed ? "-" : NumberUtil.formatRaw(fiberCost))
                    .replace("{driftwood_cost}",   maxed ? "-" : (dwood  > 0 ? NumberUtil.formatRaw(dwood)  : "0"))
                    .replace("{moss_cost}",        maxed ? "-" : (moss   > 0 ? NumberUtil.formatRaw(moss)   : "0"))
                    .replace("{reed_cost}",        maxed ? "-" : (reed   > 0 ? NumberUtil.formatRaw(reed)   : "0"))
                    .replace("{clover_cost}",      maxed ? "-" : (clover > 0 ? NumberUtil.formatRaw(clover) : "0"))
                    .replace("{has_fiber}",        colorCheck(data.getFiber()     >= fiberCost || maxed))
                    .replace("{has_driftwood}",    colorCheck(data.getDriftwood() >= dwood  || dwood  == 0 || maxed))
                    .replace("{has_moss}",         colorCheck(data.getMoss()      >= moss   || moss   == 0 || maxed))
                    .replace("{has_reed}",         colorCheck(data.getReed()      >= reed   || reed   == 0 || maxed))
                    .replace("{has_clover}",       colorCheck(data.getClover()    >= clover || clover == 0 || maxed))
            ));
        }

        return GuiUtil.createItem(mat, rawName, lore);
    }

    private ItemStack buildResourceInfo(PlayerData data, ConfigurationSection sec) {
        ConfigurationSection infoSec = sec.getConfigurationSection("info");
        Material mat = infoSec != null
                ? parseMaterial(infoSec.getString("material"), Material.AMETHYST_SHARD)
                : Material.AMETHYST_SHARD;
        String name = infoSec != null
                ? infoSec.getString("name", "&#d4a8ff&lYour Resources")
                : "&#d4a8ff&lYour Resources";
        List<String> loreTemplate = infoSec != null
                ? infoSec.getStringList("lore")
                : List.of();

        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ColorUtil.translate(line
                    .replace("{fiber}",     NumberUtil.formatRaw(data.getFiber()))
                    .replace("{driftwood}", NumberUtil.formatRaw(data.getDriftwood()))
                    .replace("{moss}",      NumberUtil.formatRaw(data.getMoss()))
                    .replace("{reed}",      NumberUtil.formatRaw(data.getReed()))
                    .replace("{clover}",    NumberUtil.formatRaw(data.getClover()))
            ));
        }
        return GuiUtil.createItem(mat, name, lore);
    }

    // ── Helpers ────────────────────────────────────────────────

    private int defaultSlot(ElderManager.ElderPerkType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> 10;
            case MATERIAL_AMOUNT -> 12;
            case XP_GAIN         -> 14;
            case MATERIAL_CHANCE -> 16;
        };
    }

    private Material perkMaterialFallback(ElderManager.ElderPerkType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> Material.WHEAT;
            case MATERIAL_AMOUNT -> Material.MOSS_BLOCK;
            case XP_GAIN         -> Material.EXPERIENCE_BOTTLE;
            case MATERIAL_CHANCE -> Material.PITCHER_PLANT;
        };
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : fallback;
    }

    private String colorCheck(boolean has) {
        return has ? "&#a8ff78✔" : "&#ff7a7a✘";
    }

    // Slot accessors — read live from config so changes take effect on reload
    public int getSlotFiber()    { return getSlot("fiber_amount",    10); }
    public int getSlotMaterial() { return getSlot("material_amount", 12); }
    public int getSlotXp()       { return getSlot("xp_gain",         14); }
    public int getSlotChance()   { return getSlot("material_chance", 16); }

    public int getSlotClose() {
        ConfigurationSection sec = plugin.getConfigManager().getElderConfig()
                .getConfigurationSection("elder-menu");
        if (sec == null) return 35;
        return sec.getInt("close-button.slot", 35);
    }

    private int getSlot(String perkKey, int def) {
        ConfigurationSection sec = plugin.getConfigManager().getElderConfig()
                .getConfigurationSection("elder-menu");
        if (sec == null) return def;
        return sec.getInt("perks." + perkKey + ".slot", def);
    }
}