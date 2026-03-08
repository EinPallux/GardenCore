package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.entity.Player;

import java.util.UUID;

public class FiberManager {

    private final GardenCore plugin;

    public FiberManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public double getFiber(UUID uuid) {
        return plugin.getDataManager().getPlayerData(uuid).getFiber();
    }

    public void setFiber(UUID uuid, double amount) {
        plugin.getDataManager().getPlayerData(uuid).setFiber(amount);
    }

    public void giveFiber(UUID uuid, double amount) {
        plugin.getDataManager().getPlayerData(uuid).addFiber(amount);
    }

    public void takeFiber(UUID uuid, double amount) {
        plugin.getDataManager().getPlayerData(uuid).takeFiber(amount);
    }

    public boolean hasFiber(UUID uuid, double amount) {
        return getFiber(uuid) >= amount;
    }

    public String formatRaw(UUID uuid) {
        return NumberUtil.formatRaw(getFiber(uuid));
    }

    public String formatFormatted(UUID uuid) {
        return NumberUtil.formatRaw(getFiber(uuid));
    }

    public void addFiberFromCrop(Player player, double baseFiber) {
        double totalMultiplier = plugin.getMultiplierManager().getTotalFiberMultiplier(player.getUniqueId());
        double earned = baseFiber * totalMultiplier;
        giveFiber(player.getUniqueId(), earned);
    }
}
