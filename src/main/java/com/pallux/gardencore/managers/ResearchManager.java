package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.MessageUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;

public class ResearchManager {

    private final GardenCore plugin;
    private BukkitTask checkTask;

    public ResearchManager(GardenCore plugin) {
        this.plugin = plugin;
        startCompletionChecker();
    }

    private void startCompletionChecker() {
        checkTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                checkCompletion(player);
            }
        }, 100L, 100L);
    }

    public void checkCompletion(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasActiveResearch()) return;

        long durationMs = getDurationMs(data.getActiveResearchIndex());
        long elapsed = System.currentTimeMillis() - data.getActiveResearchStart();

        if (elapsed >= durationMs) {
            completeResearch(player, data);
        }
    }

    private void completeResearch(Player player, PlayerData data) {
        int index = data.getActiveResearchIndex();
        data.setCompletedResearches(index + 1);
        data.setActiveResearchIndex(-1);
        data.setActiveResearchStart(0);
        plugin.getDataManager().saveAsync();

        double fiberPerResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.fiber-amount-per-research", 500.0);
        double materialPerResearch = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.material-amount-per-research", 1.0);

        double fiberBonus = (index + 1) * fiberPerResearch;
        double materialBonus = (index + 1) * materialPerResearch;

        MessageUtil.send(player, "research.completed", Map.of(
                "fiber_multi", String.format("%.1f", fiberBonus),
                "material_multi", String.format("%.1f", materialBonus)
        ));
    }

    public boolean startResearch(Player player, int index) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (data.hasActiveResearch()) {
            MessageUtil.send(player, "research.already-active");
            return false;
        }

        if (index < data.getCompletedResearches()) {
            MessageUtil.send(player, "research.already-completed");
            return false;
        }

        if (index > data.getCompletedResearches()) {
            MessageUtil.send(player, "research.locked");
            return false;
        }

        double cost = getCost(index);
        if (data.getFiber() < cost) {
            MessageUtil.send(player, "research.not-enough-fiber", Map.of(
                    "cost", NumberUtil.formatRaw(cost),
                    "balance", NumberUtil.formatRaw(data.getFiber())
            ));
            return false;
        }

        data.takeFiber(cost);
        data.setActiveResearchIndex(index);
        data.setActiveResearchStart(System.currentTimeMillis());
        plugin.getDataManager().saveAsync();

        MessageUtil.send(player, "research.started", Map.of(
                "time", formatDuration(getDurationMs(index) / 1000)
        ));
        return true;
    }

    public boolean cancelResearch(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (!data.hasActiveResearch()) return false;

        data.setActiveResearchIndex(-1);
        data.setActiveResearchStart(0);
        plugin.getDataManager().saveAsync();

        MessageUtil.send(player, "research.cancelled");
        return true;
    }

    public int getTotalResearches() {
        return plugin.getConfigManager().getResearchConfig().getInt("research.total-researches", 28);
    }

    public long getDurationMs(int index) {
        int baseMinutes = plugin.getConfigManager().getResearchConfig()
                .getInt("research.base-duration-minutes", 10);
        long minutes = (long) baseMinutes * (index + 1);
        return minutes * 60 * 1000;
    }

    public long getTimeRemainingMs(PlayerData data) {
        if (!data.hasActiveResearch()) return 0;
        long durationMs = getDurationMs(data.getActiveResearchIndex());
        long elapsed = System.currentTimeMillis() - data.getActiveResearchStart();
        return Math.max(0, durationMs - elapsed);
    }

    /**
     * Cost formula: base-cost × cost-growth^index
     * Matches researchmenu.yml: base-cost: 500, cost-growth: 1.45
     *   R1  (index 0) =   500
     *   R5  (index 4) = 2,210
     *   R10 (index 9) = 14,167
     *   R28 (index 27) ≈ 11,373,813
     */
    public double getCost(int index) {
        double base   = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.base-cost", 500.0);
        double growth = plugin.getConfigManager().getResearchConfig()
                .getDouble("research.cost-growth", 1.45);
        return Math.round(base * Math.pow(growth, index));
    }

    public String formatDuration(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        if (hours > 0) return String.format("%dh %dm %ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds", minutes, seconds);
        return String.format("%ds", seconds);
    }

    public String getResearchName(int index) {
        return "Research " + toRoman(index + 1);
    }

    private String toRoman(int num) {
        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return thousands[num / 1000] + hundreds[(num % 1000) / 100] + tens[(num % 100) / 10] + ones[num % 10];
    }

    public void shutdown() {
        if (checkTask != null) checkTask.cancel();
    }
}