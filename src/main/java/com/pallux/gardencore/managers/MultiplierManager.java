package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.EventData;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.NumberUtil;

import java.util.UUID;

public class MultiplierManager {

    private final GardenCore plugin;

    public MultiplierManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public double getTotalFiberMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base          = 1.0;
        double upgradeBonus  = data.getFiberAmountUpgrade() * 0.1;
        double adminBonus    = data.getBonusFiberMultiplier() / 100.0;
        double eventBonus    = getEventBonus(EventData.EventType.FIBER_AMOUNT);
        double researchBonus = getResearchFiberBonus(uuid);
        double elderBonus    = plugin.getElderManager().getElderFiberBonus(uuid);
        return base + upgradeBonus + adminBonus + (eventBonus / 100.0) + researchBonus + elderBonus;
    }

    public double getTotalMaterialAmountMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base          = 1.0;
        double upgradeBonus  = data.getMaterialAmountUpgrade() * 0.1;
        double adminBonus    = data.getBonusMaterialAmountMultiplier() / 100.0;
        double eventBonus    = getEventBonus(EventData.EventType.MATERIAL_AMOUNT);
        double researchBonus = getResearchMaterialBonus(uuid);
        double elderBonus    = plugin.getElderManager().getElderMaterialAmountBonus(uuid);
        return base + upgradeBonus + adminBonus + (eventBonus / 100.0) + researchBonus + elderBonus;
    }

    public double getTotalMaterialChanceMultiplier(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double base         = 1.0;
        double upgradeBonus = data.getMaterialChanceUpgrade() * 0.01;
        double adminBonus   = data.getBonusMaterialChanceMultiplier() / 100.0;
        double eventBonus   = getEventBonus(EventData.EventType.MATERIAL_CHANCE);
        double elderBonus   = plugin.getElderManager().getElderMaterialChanceBonus(uuid);
        return base + upgradeBonus + adminBonus + (eventBonus / 100.0) + elderBonus;
    }

    /** Returns the total XP multiplier including Elder bonus. Used by LevelManager. */
    public double getTotalXpMultiplier(UUID uuid) {
        double eventBonus = getEventBonus(EventData.EventType.XP_AMOUNT);
        double elderBonus = plugin.getElderManager().getElderXpBonus(uuid);
        return 1.0 + (eventBonus / 100.0) + elderBonus;
    }

    private double getEventBonus(EventData.EventType type) {
        var eventManager = plugin.getEventManager();
        if (eventManager == null) return 0;
        return eventManager.getTotalEventBonus(type);
    }

    // Research bonus is cumulative based on completed count
    private double getResearchFiberBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double perResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.fiber-amount-per-research", 0.1);
        return completed * perResearch;
    }

    private double getResearchMaterialBonus(UUID uuid) {
        int completed = plugin.getDataManager().getPlayerData(uuid).getCompletedResearches();
        if (completed == 0) return 0;
        double perResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.material-amount-per-research", 0.1);
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