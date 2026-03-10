package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class LevelManager {

    private final GardenCore plugin;

    public LevelManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── XP formula ────────────────────────────────────────────

    /**
     * Returns the XP required to level up from {@code level} to {@code level + 1}.
     * Three modes are supported, all configured in settings/leveling.yml:
     *   linear    — base * level
     *   flat      — base  (same cost every level)
     *   quadratic — base * level^exponent
     */
    public double getXpRequired(int level) {
        FileConfiguration cfg = plugin.getConfigManager().getLevelingConfig();
        String mode  = cfg.getString("leveling.xp-formula.mode", "linear").toLowerCase();
        double base  = cfg.getDouble("leveling.xp-formula.base", 100.0);
        return switch (mode) {
            case "flat"      -> base;
            case "quadratic" -> {
                double exp = cfg.getDouble("leveling.xp-formula.exponent", 2.0);
                yield base * Math.pow(level, exp);
            }
            default          -> base * level; // linear
        };
    }

    // ── XP granting ───────────────────────────────────────────

    public void addXp(Player player, double xp) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        double multiplied = xp * plugin.getMultiplierManager().getTotalXpMultiplier(player.getUniqueId());
        data.addXp(multiplied);
        checkLevelUp(player, data);
    }

    public void addXpDirect(Player player, double xp) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.addXp(xp);
        checkLevelUp(player, data);
    }

    // ── Level-up logic ────────────────────────────────────────

    private void checkLevelUp(Player player, PlayerData data) {
        boolean leveled = false;
        while (data.getXp() >= getXpRequired(data.getLevel())) {
            data.setXp(data.getXp() - getXpRequired(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            leveled = true;
        }

        if (!leveled) return;

        int newLevel = data.getLevel();

        // Personal chat message
        MessageUtil.send(player, "level.level-up", Map.of("level", String.valueOf(newLevel)));

        // On-screen title (configured in leveling.yml)
        sendLevelUpTitle(player, newLevel);

        // Server-wide milestone broadcast
        FileConfiguration cfg = plugin.getConfigManager().getLevelingConfig();
        int milestone = cfg.getInt("leveling.broadcast-milestone", 10);
        if (milestone > 0 && newLevel % milestone == 0) {
            String broadcastMsg = plugin.getConfigManager().getMessage("level.broadcast")
                    .replace("{player}", player.getName())
                    .replace("{level}", String.valueOf(newLevel));
            MessageUtil.broadcast(broadcastMsg);
        }
    }

    private void sendLevelUpTitle(Player player, int newLevel) {
        FileConfiguration cfg = plugin.getConfigManager().getLevelingConfig();
        String titleRaw    = cfg.getString("leveling.level-up-title",    "&#a8ff78&l✦ Level Up!");
        String subtitleRaw = cfg.getString("leveling.level-up-subtitle", "&7You reached &a&lLevel {level}&7!");

        String title    = ColorUtil.translate(titleRaw.replace("{level}", String.valueOf(newLevel)));
        String subtitle = ColorUtil.translate(subtitleRaw.replace("{level}", String.valueOf(newLevel)));

        // Skip sending if both are blank
        if (title.isBlank() && subtitle.isBlank()) return;
        player.sendTitle(title, subtitle, 5, 40, 10);
    }

    // ── Accessors ─────────────────────────────────────────────

    public int getLevel(UUID uuid) {
        return plugin.getDataManager().getPlayerData(uuid).getLevel();
    }

    public double getXpPercent(UUID uuid) {
        PlayerData data = plugin.getDataManager().getPlayerData(uuid);
        double required = getXpRequired(data.getLevel());
        if (required == 0) return 100;
        return (data.getXp() / required) * 100.0;
    }
}