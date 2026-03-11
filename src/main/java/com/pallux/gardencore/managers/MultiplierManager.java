package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.EventData;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.UUID;

public class MultiplierManager {

    public static final double FIBER_BONUS_PER_UPGRADE    = 25.0;
    public static final double MATERIAL_BONUS_PER_UPGRADE = 0.15;
    public static final double CHANCE_BONUS_PER_UPGRADE   = 0.04;

    // Permission prefixes for donor rank bonuses.
    // Full node format: gc.multi.<type>.donor.<percent>
    // e.g. gc.multi.fiber_amount.donor.50  → +50% of the final fiber multiplier
    private static final String PERM_FIBER_AMOUNT    = "gc.multi.fiber_amount.donor.";
    private static final String PERM_MATERIAL_AMOUNT = "gc.multi.material_amount.donor.";
    private static final String PERM_XP              = "gc.multi.xp.donor.";
    private static final String PERM_MATERIAL_CHANCE = "gc.multi.material_chance.donor.";

    private final GardenCore plugin;

    public MultiplierManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Fiber multiplier ──────────────────────────────────────────────────────
    // Base total is calculated first, then scaled by the donor percentage bonus.
    public double getTotalFiberMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base = 1.0
                + data.getFiberAmountUpgrade()   * FIBER_BONUS_PER_UPGRADE
                + data.getBonusFiberMultiplier() / 100.0
                + getEventBonus(EventData.EventType.FIBER_AMOUNT) / 100.0
                + getResearchFiberBonus(uuid)
                + plugin.getElderManager().getElderFiberBonus(uuid)
                + plugin.getPetManager().getPetFiberBonus(uuid)
                + getComposterFiberBonus(uuid) / 100.0;
        return base * (1.0 + getDonorBonus(uuid, PERM_FIBER_AMOUNT) / 100.0);
    }

    // ── Material amount multiplier ────────────────────────────────────────────
    public double getTotalMaterialAmountMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base = 1.0
                + data.getMaterialAmountUpgrade()         * MATERIAL_BONUS_PER_UPGRADE
                + data.getBonusMaterialAmountMultiplier() / 100.0
                + getEventBonus(EventData.EventType.MATERIAL_AMOUNT) / 100.0
                + getResearchMaterialBonus(uuid)
                + plugin.getElderManager().getElderMaterialAmountBonus(uuid)
                + getComposterMaterialBonus(uuid) / 100.0;
        return base * (1.0 + getDonorBonus(uuid, PERM_MATERIAL_AMOUNT) / 100.0);
    }

    // ── Material chance multiplier ────────────────────────────────────────────
    public double getTotalMaterialChanceMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base = 1.0
                + data.getMaterialChanceUpgrade()         * CHANCE_BONUS_PER_UPGRADE
                + data.getBonusMaterialChanceMultiplier() / 100.0
                + getEventBonus(EventData.EventType.MATERIAL_CHANCE) / 100.0
                + plugin.getElderManager().getElderMaterialChanceBonus(uuid);
        return base * (1.0 + getDonorBonus(uuid, PERM_MATERIAL_CHANCE) / 100.0);
    }

    // ── XP multiplier ─────────────────────────────────────────────────────────
    public double getTotalXpMultiplier(UUID uuid) {
        double base = 1.0
                + getEventBonus(EventData.EventType.XP_AMOUNT) / 100.0
                + plugin.getElderManager().getElderXpBonus(uuid)
                + getComposterXpBonus(uuid) / 100.0;
        return base * (1.0 + getDonorBonus(uuid, PERM_XP) / 100.0);
    }

    // ── Donor permission bonus ────────────────────────────────────────────────
    /**
     * Scans all effective permissions on the online player and sums up every
     * value attached to nodes that start with {@code prefix}.
     *
     * A node like "gc.multi.fiber_amount.donor.50" with prefix
     * "gc.multi.fiber_amount.donor." yields 50.0.
     *
     * Multiple matching permissions are additive, so a player holding both
     * .donor.25 and .donor.50 receives a combined +75% bonus.
     *
     * The returned value is a raw percentage (e.g. 50.0 means 50%).
     * Each multiplier method applies it as: base * (1 + result / 100).
     *
     * If the player is offline the bonus is 0 (no permissions available).
     */
    private double getDonorBonus(UUID uuid, String prefix) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 0.0;

        double total = 0.0;
        for (PermissionAttachmentInfo pai : player.getEffectivePermissions()) {
            if (!pai.getValue()) continue; // skip negated permissions
            String perm = pai.getPermission().toLowerCase();
            if (!perm.startsWith(prefix)) continue;
            String suffix = perm.substring(prefix.length());
            try {
                double value = Double.parseDouble(suffix);
                if (value > 0) total += value;
            } catch (NumberFormatException ignored) {
                // Malformed node — skip silently
            }
        }
        return total;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double getEventBonus(EventData.EventType type) {
        var em = plugin.getEventManager();
        if (em == null) return 0;
        return em.getTotalEventBonus(type);
    }

    private double getResearchFiberBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double perResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.fiber-amount-per-research", 500.0);
        return completed * perResearch;
    }

    private double getResearchMaterialBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double perResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.material-amount-per-research", 1.0);
        return completed * perResearch;
    }

    // ── Composter bonus helpers ───────────────────────────────────────────────

    private double getComposterFiberBonus(UUID uuid) {
        Location loc = getPlayerLocation(uuid);
        if (loc == null) return 0;
        return plugin.getComposterManager().getTotalFiberBonus(loc);
    }

    private double getComposterXpBonus(UUID uuid) {
        Location loc = getPlayerLocation(uuid);
        if (loc == null) return 0;
        return plugin.getComposterManager().getTotalXpBonus(loc);
    }

    private double getComposterMaterialBonus(UUID uuid) {
        Location loc = getPlayerLocation(uuid);
        if (loc == null) return 0;
        return plugin.getComposterManager().getTotalMaterialAmountBonus(loc);
    }

    private Location getPlayerLocation(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        return player != null ? player.getLocation() : null;
    }

    public String formatFiberMultiplier(UUID uuid) {
        return NumberUtil.formatMultiplier(getTotalFiberMultiplier(uuid));
    }

    public String formatMaterialAmountMultiplier(UUID uuid) {
        return NumberUtil.formatMultiplier(getTotalMaterialAmountMultiplier(uuid));
    }

    public String formatMaterialChanceMultiplier(UUID uuid) {
        return NumberUtil.formatMultiplier(getTotalMaterialChanceMultiplier(uuid));
    }
}