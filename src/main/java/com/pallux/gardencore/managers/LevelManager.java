package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

public class LevelManager {

    private final GardenCore plugin;

    public LevelManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public double getXpRequired(int level) {
        return level * 100.0;
    }

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

    private void checkLevelUp(Player player, PlayerData data) {
        boolean leveled = false;
        while (data.getXp() >= getXpRequired(data.getLevel())) {
            data.setXp(data.getXp() - getXpRequired(data.getLevel()));
            data.setLevel(data.getLevel() + 1);
            leveled = true;
        }

        if (leveled) {
            int milestone = plugin.getConfigManager().getLevelMilestone();
            if (data.getLevel() % milestone == 0) {
                String broadcastMsg = plugin.getConfigManager().getMessage("level.broadcast");
                broadcastMsg = broadcastMsg
                        .replace("{player}", player.getName())
                        .replace("{level}", String.valueOf(data.getLevel()));
                MessageUtil.broadcast(broadcastMsg);
            }
        }
    }

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