package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class MaterialManager {

    private final GardenCore plugin;
    private final Random random = new Random();

    private double driftwoodChance;
    private double mossChance;
    private double reedChance;
    private double cloverChance;
    private int driftwoodAmount;
    private int mossAmount;
    private int reedAmount;
    private int cloverAmount;

    public MaterialManager(GardenCore plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        var cfg = plugin.getConfigManager().getMaterials();
        driftwoodChance = cfg.getDouble("materials.driftwood.drop-chance", 15.0);
        mossChance = cfg.getDouble("materials.moss.drop-chance", 8.0);
        reedChance = cfg.getDouble("materials.reed.drop-chance", 4.0);
        cloverChance = cfg.getDouble("materials.clover.drop-chance", 1.5);
        driftwoodAmount = cfg.getInt("materials.driftwood.base-amount", 1);
        mossAmount = cfg.getInt("materials.moss.base-amount", 1);
        reedAmount = cfg.getInt("materials.reed.base-amount", 1);
        cloverAmount = cfg.getInt("materials.clover.base-amount", 1);
    }

    public void rollDrops(Player player) {
        UUID uuid = player.getUniqueId();
        double chanceMultiplier = plugin.getMultiplierManager().getTotalMaterialChanceMultiplier(uuid);
        double amountMultiplier = plugin.getMultiplierManager().getTotalMaterialAmountMultiplier(uuid);

        tryDrop(player, driftwoodChance * chanceMultiplier, driftwoodAmount * amountMultiplier, "driftwood");
        tryDrop(player, mossChance * chanceMultiplier, mossAmount * amountMultiplier, "moss");
        tryDrop(player, reedChance * chanceMultiplier, reedAmount * amountMultiplier, "reed");
        tryDrop(player, cloverChance * chanceMultiplier, cloverAmount * amountMultiplier, "clover");
    }

    private void tryDrop(Player player, double chance, double amount, String material) {
        if (random.nextDouble() * 100 < chance) {
            UUID uuid = player.getUniqueId();
            PlayerData data = plugin.getDataManager().getPlayerData(uuid);
            switch (material) {
                case "driftwood" -> data.addDriftwood(amount);
                case "moss" -> data.addMoss(amount);
                case "reed" -> data.addReed(amount);
                case "clover" -> data.addClover(amount);
            }
            sendMaterialActionBar(player, material, amount);
        }
    }

    private void sendMaterialActionBar(Player player, String material, double amount) {
        String amountStr = (amount == Math.floor(amount))
                ? String.valueOf((int) amount)
                : NumberUtil.formatRaw(amount);

        String displayName = plugin.getConfigManager().getMaterials()
                .getString("materials." + material + ".display-name", material);

        String template = plugin.getConfigManager().getMessage("material.found-actionbar");
        String message = ColorUtil.translate(template
                .replace("{amount}", amountStr)
                .replace("{material}", displayName));

        player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(message));
    }

    public String formatRaw(UUID uuid, String material) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        return switch (material) {
            case "driftwood" -> NumberUtil.formatRaw(data.getDriftwood());
            case "moss" -> NumberUtil.formatRaw(data.getMoss());
            case "reed" -> NumberUtil.formatRaw(data.getReed());
            case "clover" -> NumberUtil.formatRaw(data.getClover());
            default -> "0";
        };
    }

    public String formatFormatted(UUID uuid, String material) {
        return formatRaw(uuid, material);
    }
}