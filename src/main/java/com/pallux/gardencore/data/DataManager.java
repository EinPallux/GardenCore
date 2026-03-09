package com.pallux.gardencore.data;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final GardenCore plugin;
    private final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public DataManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        dataFolder.mkdirs();

        dataFile = new File(dataFolder, "playerdata.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data/playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        for (String uuidStr : dataConfig.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                PlayerData data = new PlayerData(uuid);
                String path = uuidStr + ".";

                data.setFiber(dataConfig.getDouble(path + "fiber", 0));
                data.setXp(dataConfig.getDouble(path + "xp", 0));
                data.setLevel(dataConfig.getInt(path + "level", 1));
                data.setFiberAmountUpgrade(dataConfig.getInt(path + "upgrades.fiber-amount", 0));
                data.setMaterialAmountUpgrade(dataConfig.getInt(path + "upgrades.material-amount", 0));
                data.setMaterialChanceUpgrade(dataConfig.getInt(path + "upgrades.material-chance", 0));
                data.setCropCooldownUpgrade(dataConfig.getInt(path + "upgrades.crop-cooldown", 0));
                data.setBonusFiberMultiplier(dataConfig.getDouble(path + "bonus.fiber-multiplier", 0));
                data.setBonusMaterialAmountMultiplier(dataConfig.getDouble(path + "bonus.material-amount-multiplier", 0));
                data.setBonusMaterialChanceMultiplier(dataConfig.getDouble(path + "bonus.material-chance-multiplier", 0));
                data.setDriftwood(dataConfig.getDouble(path + "materials.driftwood", 0));
                data.setMoss(dataConfig.getDouble(path + "materials.moss", 0));
                data.setReed(dataConfig.getDouble(path + "materials.reed", 0));
                data.setClover(dataConfig.getDouble(path + "materials.clover", 0));
                data.setCompletedResearches(dataConfig.getInt(path + "research.completed", 0));
                data.setActiveResearchIndex(dataConfig.getInt(path + "research.active-index", -1));
                data.setActiveResearchStart(dataConfig.getLong(path + "research.active-start", 0));
                // Elder perks
                data.setElderFiberLevel(dataConfig.getInt(path + "elder.fiber-level", 0));
                data.setElderMaterialAmountLevel(dataConfig.getInt(path + "elder.material-amount-level", 0));
                data.setElderXpGainLevel(dataConfig.getInt(path + "elder.xp-gain-level", 0));
                data.setElderMaterialChanceLevel(dataConfig.getInt(path + "elder.material-chance-level", 0));

                playerDataMap.put(uuid, data);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    public void saveAll() {
        for (PlayerData data : playerDataMap.values()) {
            save(data);
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data/playerdata.yml: " + e.getMessage());
        }
    }

    public void save(PlayerData data) {
        String path = data.getUuid().toString() + ".";
        dataConfig.set(path + "fiber", data.getFiber());
        dataConfig.set(path + "xp", data.getXp());
        dataConfig.set(path + "level", data.getLevel());
        dataConfig.set(path + "upgrades.fiber-amount", data.getFiberAmountUpgrade());
        dataConfig.set(path + "upgrades.material-amount", data.getMaterialAmountUpgrade());
        dataConfig.set(path + "upgrades.material-chance", data.getMaterialChanceUpgrade());
        dataConfig.set(path + "upgrades.crop-cooldown", data.getCropCooldownUpgrade());
        dataConfig.set(path + "bonus.fiber-multiplier", data.getBonusFiberMultiplier());
        dataConfig.set(path + "bonus.material-amount-multiplier", data.getBonusMaterialAmountMultiplier());
        dataConfig.set(path + "bonus.material-chance-multiplier", data.getBonusMaterialChanceMultiplier());
        dataConfig.set(path + "materials.driftwood", data.getDriftwood());
        dataConfig.set(path + "materials.moss", data.getMoss());
        dataConfig.set(path + "materials.reed", data.getReed());
        dataConfig.set(path + "materials.clover", data.getClover());
        dataConfig.set(path + "research.completed", data.getCompletedResearches());
        dataConfig.set(path + "research.active-index", data.getActiveResearchIndex());
        dataConfig.set(path + "research.active-start", data.getActiveResearchStart());
        // Elder perks
        dataConfig.set(path + "elder.fiber-level", data.getElderFiberLevel());
        dataConfig.set(path + "elder.material-amount-level", data.getElderMaterialAmountLevel());
        dataConfig.set(path + "elder.xp-gain-level", data.getElderXpGainLevel());
        dataConfig.set(path + "elder.material-chance-level", data.getElderMaterialChanceLevel());
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveAll);
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public boolean hasPlayerData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }

    /**
     * Saves and unloads a player's data asynchronously.
     * The data is written to the config on the main thread (to avoid concurrent map access),
     * then the file write is dispatched async to avoid blocking on quit.
     */
    public void removePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.remove(uuid);
        if (data == null) return;

        // Update the in-memory YAML config synchronously (safe — main thread)
        save(data);

        // Write to disk asynchronously so the main thread is not blocked on quit
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save data/playerdata.yml on quit: " + e.getMessage());
            }
        });
    }
}