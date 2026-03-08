package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import org.bukkit.entity.Player;

public class UpgradeManager {

    public static final int MAX_FIBER_AMOUNT = 1000;
    public static final int MAX_MATERIAL_AMOUNT = 500;
    public static final int MAX_MATERIAL_CHANCE = 100;
    public static final int MAX_CROP_COOLDOWN = 15;

    public static final double BASE_CROP_COOLDOWN = 2.0;
    public static final double MIN_CROP_COOLDOWN = 0.5;
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

    public double getUpgradeCost(UpgradeType type, int currentLevel) {
        return switch (type) {
            case FIBER_AMOUNT -> 100.0 + (currentLevel * 10);
            case MATERIAL_AMOUNT -> 200.0 + (currentLevel * 20);
            case MATERIAL_CHANCE -> 500.0 + (currentLevel * 50);
            case CROP_COOLDOWN -> 300.0 + (currentLevel * 30);
        };
    }

    public int getMaxLevel(UpgradeType type) {
        return switch (type) {
            case FIBER_AMOUNT -> MAX_FIBER_AMOUNT;
            case MATERIAL_AMOUNT -> MAX_MATERIAL_AMOUNT;
            case MATERIAL_CHANCE -> MAX_MATERIAL_CHANCE;
            case CROP_COOLDOWN -> MAX_CROP_COOLDOWN;
        };
    }

    public int getCurrentLevel(Player player, UpgradeType type) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        return switch (type) {
            case FIBER_AMOUNT -> data.getFiberAmountUpgrade();
            case MATERIAL_AMOUNT -> data.getMaterialAmountUpgrade();
            case MATERIAL_CHANCE -> data.getMaterialChanceUpgrade();
            case CROP_COOLDOWN -> data.getCropCooldownUpgrade();
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
            case FIBER_AMOUNT -> data.setFiberAmountUpgrade(current + 1);
            case MATERIAL_AMOUNT -> data.setMaterialAmountUpgrade(current + 1);
            case MATERIAL_CHANCE -> data.setMaterialChanceUpgrade(current + 1);
            case CROP_COOLDOWN -> data.setCropCooldownUpgrade(current + 1);
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
            case FIBER_AMOUNT -> "Fiber Amount";
            case MATERIAL_AMOUNT -> "Material Amount";
            case MATERIAL_CHANCE -> "Material Chance";
            case CROP_COOLDOWN -> "Crop Cooldown";
        };
    }

    public static UpgradeType fromString(String s) {
        return switch (s.toLowerCase()) {
            case "fiber_amount" -> UpgradeType.FIBER_AMOUNT;
            case "material_amount" -> UpgradeType.MATERIAL_AMOUNT;
            case "material_chance" -> UpgradeType.MATERIAL_CHANCE;
            case "crop_cooldown" -> UpgradeType.CROP_COOLDOWN;
            default -> null;
        };
    }
}