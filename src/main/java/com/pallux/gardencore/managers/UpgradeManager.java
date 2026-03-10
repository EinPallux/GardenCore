package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import org.bukkit.entity.Player;

public class UpgradeManager {

    // ── Max levels ────────────────────────────────────────────────────────────
    // Fiber Amount:    200 levels  each +25x   → +5,000x total
    // Material Amount: 100 levels  each +0.15x → +15x    total
    // Material Chance:  50 levels  each +0.04x → +2x     total (intentionally small)
    // Crop Cooldown:     8 levels  each -0.1s  → 1.0s → 0.2s minimum
    public static final int MAX_FIBER_AMOUNT    = 200;
    public static final int MAX_MATERIAL_AMOUNT = 100;
    public static final int MAX_MATERIAL_CHANCE =  50;
    public static final int MAX_CROP_COOLDOWN   =   8;

    public static final double BASE_CROP_COOLDOWN           = 1.0;
    public static final double MIN_CROP_COOLDOWN            = 0.2;
    public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.1;

    public enum UpgradeType {
        FIBER_AMOUNT,
        MATERIAL_AMOUNT,
        MATERIAL_CHANCE,
        CROP_COOLDOWN
    }

    private final GardenCore plugin;

    public UpgradeManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Cost formula: base × level^exp — cheap early, scales hard late.
     * L1 is affordable within the first minute; L200 requires serious late-game income.
     *
     *  FIBER_AMOUNT     base=50   × level^1.9
     *    L1=50   L5=1.1K   L20=14.8K   L50=84.5K   L100=315K   L200=1.18M
     *    Total all 200 levels ≈ 81.8M fiber
     *
     *  MATERIAL_AMOUNT  base=150  × level^1.9
     *    L1=150  L5=3.2K   L20=44.5K   L50=254K    L100=946K
     *    Total all 100 levels ≈ 33.1M fiber
     *
     *  MATERIAL_CHANCE  base=500  × level^2.2
     *    L1=500  L5=17K    L10=79K     L25=595K    L50=2.73M
     *    Total all 50 levels ≈ 44M fiber  (expensive luxury)
     *
     *  CROP_COOLDOWN    base=200  × level^1.9
     *    L1=200  L4=2.8K   L8=10.4K
     *    Total all 8 levels ≈ 34K fiber
     */
    public double getUpgradeCost(UpgradeType type, int currentLevel) {
        double lvl = currentLevel + 1.0;
        return switch (type) {
            case FIBER_AMOUNT    -> Math.round(50.0  * Math.pow(lvl, 1.9));
            case MATERIAL_AMOUNT -> Math.round(150.0 * Math.pow(lvl, 1.9));
            case MATERIAL_CHANCE -> Math.round(500.0 * Math.pow(lvl, 2.2));
            case CROP_COOLDOWN   -> Math.round(200.0 * Math.pow(lvl, 1.9));
        };
    }

    public int getMaxLevel(UpgradeType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> MAX_FIBER_AMOUNT;
            case MATERIAL_AMOUNT -> MAX_MATERIAL_AMOUNT;
            case MATERIAL_CHANCE -> MAX_MATERIAL_CHANCE;
            case CROP_COOLDOWN   -> MAX_CROP_COOLDOWN;
        };
    }

    public int getCurrentLevel(Player player, UpgradeType type) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return switch (type) {
            case FIBER_AMOUNT    -> data.getFiberAmountUpgrade();
            case MATERIAL_AMOUNT -> data.getMaterialAmountUpgrade();
            case MATERIAL_CHANCE -> data.getMaterialChanceUpgrade();
            case CROP_COOLDOWN   -> data.getCropCooldownUpgrade();
        };
    }

    public double getEffectiveCropCooldown(Player player) {
        int level = getCurrentLevel(player, UpgradeType.CROP_COOLDOWN);
        double cooldown = BASE_CROP_COOLDOWN - (level * COOLDOWN_REDUCTION_PER_LEVEL);
        return Math.max(MIN_CROP_COOLDOWN, cooldown);
    }

    public UpgradeResult tryUpgrade(Player player, UpgradeType type) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int current = getCurrentLevel(player, type);
        int max = getMaxLevel(type);

        if (current >= max) return UpgradeResult.MAX_REACHED;

        double cost = getUpgradeCost(type, current);
        if (!plugin.getFiberManager().hasFiber(player.getUniqueId(), cost)) {
            return UpgradeResult.NOT_ENOUGH_FIBER;
        }

        plugin.getFiberManager().takeFiber(player.getUniqueId(), cost);

        switch (type) {
            case FIBER_AMOUNT    -> data.setFiberAmountUpgrade(current + 1);
            case MATERIAL_AMOUNT -> data.setMaterialAmountUpgrade(current + 1);
            case MATERIAL_CHANCE -> data.setMaterialChanceUpgrade(current + 1);
            case CROP_COOLDOWN   -> data.setCropCooldownUpgrade(current + 1);
        }

        return UpgradeResult.SUCCESS;
    }

    public enum UpgradeResult {
        SUCCESS,
        MAX_REACHED,
        NOT_ENOUGH_FIBER
    }

    public String getDisplayName(UpgradeType type) {
        return switch (type) {
            case FIBER_AMOUNT    -> "Fiber Amount";
            case MATERIAL_AMOUNT -> "Material Amount";
            case MATERIAL_CHANCE -> "Material Chance";
            case CROP_COOLDOWN   -> "Crop Cooldown";
        };
    }

    public static UpgradeType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "fiber_amount"    -> UpgradeType.FIBER_AMOUNT;
            case "material_amount" -> UpgradeType.MATERIAL_AMOUNT;
            case "material_chance" -> UpgradeType.MATERIAL_CHANCE;
            case "crop_cooldown"   -> UpgradeType.CROP_COOLDOWN;
            default -> null;
        };
    }
}