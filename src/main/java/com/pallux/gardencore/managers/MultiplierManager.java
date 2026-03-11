package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.EventData;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class MultiplierManager {

    public static final double FIBER_BONUS_PER_UPGRADE    = 25.0;
    public static final double MATERIAL_BONUS_PER_UPGRADE = 0.15;
    public static final double CHANCE_BONUS_PER_UPGRADE   = 0.04;

    private final GardenCore plugin;

    public MultiplierManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Fiber multiplier ──────────────────────────────────────────────────────
    // Sources: base(1) + upgrades + admin bonus + event + research + elder + pet + composter
    public double getTotalFiberMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        return 1.0
                + data.getFiberAmountUpgrade()   * FIBER_BONUS_PER_UPGRADE
                + data.getBonusFiberMultiplier() / 100.0
                + getEventBonus(EventData.EventType.FIBER_AMOUNT) / 100.0
                + getResearchFiberBonus(uuid)
                + plugin.getElderManager().getElderFiberBonus(uuid)
                + plugin.getPetManager().getPetFiberBonus(uuid)
                + getComposterFiberBonus(uuid) / 100.0;
    }

    // ── Material amount multiplier ────────────────────────────────────────────
    public double getTotalMaterialAmountMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        return 1.0
                + data.getMaterialAmountUpgrade()         * MATERIAL_BONUS_PER_UPGRADE
                + data.getBonusMaterialAmountMultiplier() / 100.0
                + getEventBonus(EventData.EventType.MATERIAL_AMOUNT) / 100.0
                + getResearchMaterialBonus(uuid)
                + plugin.getElderManager().getElderMaterialAmountBonus(uuid)
                + getComposterMaterialBonus(uuid) / 100.0;
    }

    // ── Material chance multiplier ────────────────────────────────────────────
    public double getTotalMaterialChanceMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        return 1.0
                + data.getMaterialChanceUpgrade()         * CHANCE_BONUS_PER_UPGRADE
                + data.getBonusMaterialChanceMultiplier() / 100.0
                + getEventBonus(EventData.EventType.MATERIAL_CHANCE) / 100.0
                + plugin.getElderManager().getElderMaterialChanceBonus(uuid);
    }

    // ── XP multiplier ─────────────────────────────────────────────────────────
    public double getTotalXpMultiplier(UUID uuid) {
        return 1.0
                + getEventBonus(EventData.EventType.XP_AMOUNT) / 100.0
                + plugin.getElderManager().getElderXpBonus(uuid)
                + getComposterXpBonus(uuid) / 100.0;
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