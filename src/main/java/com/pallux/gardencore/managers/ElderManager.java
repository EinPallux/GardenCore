package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.MessageUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ElderManager {

    public enum ElderPerkType {
        FIBER_AMOUNT,
        MATERIAL_AMOUNT,
        XP_GAIN,
        MATERIAL_CHANCE
    }

    public enum PurchaseResult {
        SUCCESS,
        MAX_REACHED,
        NOT_ENOUGH_FIBER,
        NOT_ENOUGH_DRIFTWOOD,
        NOT_ENOUGH_MOSS,
        NOT_ENOUGH_REED,
        NOT_ENOUGH_CLOVER
    }

    private final GardenCore plugin;

    public ElderManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Level queries ──────────────────────────────────────────

    public int getCurrentLevel(UUID uuid, ElderPerkType type) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        return switch (type) {
            case FIBER_AMOUNT    -> data.getElderFiberLevel();
            case MATERIAL_AMOUNT -> data.getElderMaterialAmountLevel();
            case XP_GAIN         -> data.getElderXpGainLevel();
            case MATERIAL_CHANCE -> data.getElderMaterialChanceLevel();
        };
    }

    public int getMaxLevel(ElderPerkType type) {
        return cfg().getInt("elder-menu.perks." + configKey(type) + ".max-level", 50);
    }

    // ── Cost queries ───────────────────────────────────────────

    public double getFiberCost(ElderPerkType type, int currentLevel) {
        double base  = cfg().getDouble("elder-menu.perks." + configKey(type) + ".fiber-base-cost", 500);
        double scale = cfg().getDouble("elder-menu.perks." + configKey(type) + ".fiber-cost-scale", 500);
        return base + currentLevel * scale;
    }

    public double getDriftwoodCost(ElderPerkType type, int currentLevel) {
        double base  = cfg().getDouble("elder-menu.perks." + configKey(type) + ".driftwood-base-cost", 0);
        double scale = cfg().getDouble("elder-menu.perks." + configKey(type) + ".driftwood-cost-scale", 0);
        return base + currentLevel * scale;
    }

    public double getMossCost(ElderPerkType type, int currentLevel) {
        double base  = cfg().getDouble("elder-menu.perks." + configKey(type) + ".moss-base-cost", 0);
        double scale = cfg().getDouble("elder-menu.perks." + configKey(type) + ".moss-cost-scale", 0);
        return base + currentLevel * scale;
    }

    public double getReedCost(ElderPerkType type, int currentLevel) {
        double base  = cfg().getDouble("elder-menu.perks." + configKey(type) + ".reed-base-cost", 0);
        double scale = cfg().getDouble("elder-menu.perks." + configKey(type) + ".reed-cost-scale", 0);
        return base + currentLevel * scale;
    }

    public double getCloverCost(ElderPerkType type, int currentLevel) {
        double base  = cfg().getDouble("elder-menu.perks." + configKey(type) + ".clover-base-cost", 0);
        double scale = cfg().getDouble("elder-menu.perks." + configKey(type) + ".clover-cost-scale", 0);
        return base + currentLevel * scale;
    }

    // ── Bonus value ────────────────────────────────────────────

    public double getBonusPerLevel(ElderPerkType type) {
        return cfg().getDouble("elder-menu.perks." + configKey(type) + ".bonus-per-level", 0.05);
    }

    public double getTotalBonus(UUID uuid, ElderPerkType type) {
        return getCurrentLevel(uuid, type) * getBonusPerLevel(type);
    }

    // ── Purchase ───────────────────────────────────────────────

    public PurchaseResult tryPurchase(Player player, ElderPerkType type) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        int current = getCurrentLevel(uuid, type);

        if (current >= getMaxLevel(type)) return PurchaseResult.MAX_REACHED;

        double fiber     = getFiberCost(type, current);
        double driftwood = getDriftwoodCost(type, current);
        double moss      = getMossCost(type, current);
        double reed      = getReedCost(type, current);
        double clover    = getCloverCost(type, current);

        if (data.getFiber()     < fiber)     return PurchaseResult.NOT_ENOUGH_FIBER;
        if (data.getDriftwood() < driftwood) return PurchaseResult.NOT_ENOUGH_DRIFTWOOD;
        if (data.getMoss()      < moss)      return PurchaseResult.NOT_ENOUGH_MOSS;
        if (data.getReed()      < reed)      return PurchaseResult.NOT_ENOUGH_REED;
        if (data.getClover()    < clover)    return PurchaseResult.NOT_ENOUGH_CLOVER;

        data.takeFiber(fiber);
        if (driftwood > 0) data.setDriftwood(Math.max(0, data.getDriftwood() - driftwood));
        if (moss      > 0) data.setMoss(Math.max(0, data.getMoss() - moss));
        if (reed      > 0) data.setReed(Math.max(0, data.getReed() - reed));
        if (clover    > 0) data.setClover(Math.max(0, data.getClover() - clover));

        switch (type) {
            case FIBER_AMOUNT    -> data.setElderFiberLevel(current + 1);
            case MATERIAL_AMOUNT -> data.setElderMaterialAmountLevel(current + 1);
            case XP_GAIN         -> data.setElderXpGainLevel(current + 1);
            case MATERIAL_CHANCE -> data.setElderMaterialChanceLevel(current + 1);
        }

        plugin.getDataManager().saveAsync();

        double newBonus = getTotalBonus(uuid, type) * 100;
        MessageUtil.send(player, "elder.purchased", Map.of(
                "perk",  getDisplayName(type),
                "level", String.valueOf(current + 1),
                "bonus", NumberUtil.formatRaw(newBonus)
        ));

        return PurchaseResult.SUCCESS;
    }

    // ── Multiplier accessors (used by MultiplierManager) ───────

    public double getElderFiberBonus(UUID uuid) {
        return getTotalBonus(uuid, ElderPerkType.FIBER_AMOUNT);
    }

    public double getElderMaterialAmountBonus(UUID uuid) {
        return getTotalBonus(uuid, ElderPerkType.MATERIAL_AMOUNT);
    }

    public double getElderMaterialChanceBonus(UUID uuid) {
        return getTotalBonus(uuid, ElderPerkType.MATERIAL_CHANCE);
    }

    public double getElderXpBonus(UUID uuid) {
        return getTotalBonus(uuid, ElderPerkType.XP_GAIN);
    }

    // ── Helpers ────────────────────────────────────────────────

    public String getDisplayName(ElderPerkType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> "Fiber Amount";
            case MATERIAL_AMOUNT -> "Material Amount";
            case XP_GAIN         -> "XP Gain";
            case MATERIAL_CHANCE -> "Material Chance";
        };
    }

    public String configKey(ElderPerkType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> "fiber_amount";
            case MATERIAL_AMOUNT -> "material_amount";
            case XP_GAIN         -> "xp_gain";
            case MATERIAL_CHANCE -> "material_chance";
        };
    }

    public static ElderPerkType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "fiber_amount"    -> ElderPerkType.FIBER_AMOUNT;
            case "material_amount" -> ElderPerkType.MATERIAL_AMOUNT;
            case "xp_gain"         -> ElderPerkType.XP_GAIN;
            case "material_chance" -> ElderPerkType.MATERIAL_CHANCE;
            default -> null;
        };
    }

    private FileConfiguration cfg() {
        return plugin.getConfigManager().getElderConfig();
    }
}