package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.EventData;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.NumberUtil;

import java.util.UUID;

public class MultiplierManager {

    // ── Per-upgrade-level bonuses ─────────────────────────────────────────────
    //
    // FIBER AMOUNT:    +25x   per level (200 levels max → +5,000x total)
    // MATERIAL AMOUNT: +0.15x per level (100 levels max → +15x    total)
    // MATERIAL CHANCE: +0.04x per level ( 50 levels max → +2x     total)
    //   ^ intentionally tiny — drop CHANCE should stay rare even at max
    //
    // Multiplier breakdown (all additive on base 1.0):
    //
    //   Source               Per unit    Max levels  Max bonus
    //   ──────────────────   ──────────  ──────────  ──────────────────────
    //   Upgrades             +25x/lvl    200         +5,000x
    //   Research             +500x/each  28          +14,000x
    //   Elder fiber_amount   +100x/lvl   200         +20,000x
    //   Events (temporary)   varies      —           up to ~+500x
    //   ──────────────────────────────────────────────────────
    //   TOTAL MAX:                                   ~39,001x → "x39K"
    //
    // Fiber per break at key stages (Firefly Bush base 250):
    //   U1  (50 fiber to buy):  x26    → "+6.5K Fiber"   ← instant dopamine
    //   U50:                    x1.3K  → "+312K Fiber"
    //   U200 (max upgrades):    x5K    → "+1.25M Fiber"
    //   U200 + all research:    x19K   → "+4.75M Fiber"
    //   FULL MAX:               x39K   → "+9.75M Fiber"
    //
    // Material amount max:  1 + 15 + 28 + elder = varies, ~5,700x
    // Material CHANCE max:  1 + 2 + elder_chance = ~4.5x  (Driftwood max ~22%)

    public static final double FIBER_BONUS_PER_UPGRADE    = 25.0;
    public static final double MATERIAL_BONUS_PER_UPGRADE = 0.15;
    public static final double CHANCE_BONUS_PER_UPGRADE   = 0.04;

    private final GardenCore plugin;

    public MultiplierManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Fiber multiplier ──────────────────────────────────────────────────────
    public double getTotalFiberMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base          = 1.0;
        double upgradeBonus  = data.getFiberAmountUpgrade()   * FIBER_BONUS_PER_UPGRADE;
        double adminBonus    = data.getBonusFiberMultiplier() / 100.0;
        double eventBonus    = getEventBonus(EventData.EventType.FIBER_AMOUNT) / 100.0;
        double researchBonus = getResearchFiberBonus(uuid);
        double elderBonus    = plugin.getElderManager().getElderFiberBonus(uuid);
        return base + upgradeBonus + adminBonus + eventBonus + researchBonus + elderBonus;
    }

    // ── Material amount multiplier ────────────────────────────────────────────
    public double getTotalMaterialAmountMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base          = 1.0;
        double upgradeBonus  = data.getMaterialAmountUpgrade()         * MATERIAL_BONUS_PER_UPGRADE;
        double adminBonus    = data.getBonusMaterialAmountMultiplier() / 100.0;
        double eventBonus    = getEventBonus(EventData.EventType.MATERIAL_AMOUNT) / 100.0;
        double researchBonus = getResearchMaterialBonus(uuid);
        double elderBonus    = plugin.getElderManager().getElderMaterialAmountBonus(uuid);
        return base + upgradeBonus + adminBonus + eventBonus + researchBonus + elderBonus;
    }

    // ── Material CHANCE multiplier ────────────────────────────────────────────
    // Kept very low on purpose: drop CHANCE should never feel automatic.
    // Max total: 1 + 0.75(upgrades) + 0.5(elder) = 2.25x
    public double getTotalMaterialChanceMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base         = 1.0;
        double upgradeBonus = data.getMaterialChanceUpgrade()          * CHANCE_BONUS_PER_UPGRADE;
        double adminBonus   = data.getBonusMaterialChanceMultiplier()  / 100.0;
        double eventBonus   = getEventBonus(EventData.EventType.MATERIAL_CHANCE) / 100.0;
        double elderBonus   = plugin.getElderManager().getElderMaterialChanceBonus(uuid);
        return base + upgradeBonus + adminBonus + eventBonus + elderBonus;
    }

    // ── XP multiplier ─────────────────────────────────────────────────────────
    // Elder xp_gain: 10 levels × +10x = +100x total
    // Events: up to +50% temporary
    // Max: ~1 + 100 + 0.5 = ~101.5x XP multiplier
    public double getTotalXpMultiplier(UUID uuid) {
        double eventBonus = getEventBonus(EventData.EventType.XP_AMOUNT) / 100.0;
        double elderBonus = plugin.getElderManager().getElderXpBonus(uuid);
        return 1.0 + eventBonus + elderBonus;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private double getEventBonus(EventData.EventType type) {
        var em = plugin.getEventManager();
        if (em == null) return 0;
        return em.getTotalEventBonus(type);
    }

    /**
     * Research fiber bonus: flat +500x per completed research.
     * R1=+500x ... R28=+500x each → +14,000x cumulative after all 28.
     * Simple and clear — every research feels equally impactful.
     */
    private double getResearchFiberBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double base     = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.fiber-amount-base", 50.0);
        double exponent = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.fiber-amount-exponent", 1.3);
        double total = 0;
        for (int i = 1; i <= completed; i++) {
            total += base * Math.pow(i, exponent);
        }
        return total;
    }

    /**
     * Research material bonus: each research adds 1x flat.
     * All 28 done → +28x total material amount bonus from research.
     */
    private double getResearchMaterialBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double perResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.material-amount-per-research", 1.0);
        return completed * perResearch;
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