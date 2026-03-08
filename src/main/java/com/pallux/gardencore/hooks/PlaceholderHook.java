package com.pallux.gardencore.hooks;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.NumberUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {

    private final GardenCore plugin;

    public PlaceholderHook(GardenCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "gc";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pallux";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";
        UUID uuid = player.getUniqueId();

        return switch (params.toLowerCase()) {
            case "fiber_raw" -> plugin.getFiberManager().formatRaw(uuid);
            case "fiber_formatted" -> plugin.getFiberManager().formatFormatted(uuid);
            case "level" -> String.valueOf(plugin.getLevelManager().getLevel(uuid));
            case "xp" -> NumberUtil.formatRaw(plugin.getLevelManager().getXpPercent(uuid)) + "%";
            case "driftwood_raw" -> plugin.getMaterialManager().formatRaw(uuid, "driftwood");
            case "driftwood_formatted" -> plugin.getMaterialManager().formatFormatted(uuid, "driftwood");
            case "moss_raw" -> plugin.getMaterialManager().formatRaw(uuid, "moss");
            case "moss_formatted" -> plugin.getMaterialManager().formatFormatted(uuid, "moss");
            case "reed_raw" -> plugin.getMaterialManager().formatRaw(uuid, "reed");
            case "reed_formatted" -> plugin.getMaterialManager().formatFormatted(uuid, "reed");
            case "clover_raw" -> plugin.getMaterialManager().formatRaw(uuid, "clover");
            case "clover_formatted" -> plugin.getMaterialManager().formatFormatted(uuid, "clover");
            case "multi_fiberamount" -> plugin.getMultiplierManager().formatFiberMultiplier(uuid);
            case "multi_materialamount" -> plugin.getMultiplierManager().formatMaterialAmountMultiplier(uuid);
            case "multi_materialchance" -> plugin.getMultiplierManager().formatMaterialChanceMultiplier(uuid);
            default -> null;
        };
    }
}
