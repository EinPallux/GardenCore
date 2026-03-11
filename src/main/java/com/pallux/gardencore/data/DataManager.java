package com.pallux.gardencore.data;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PetRarity;
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
    private File dataFolder;

    public DataManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Startup load ──────────────────────────────────────────

    public void load() {
        dataFolder = new File(plugin.getDataFolder(), "userdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    // ── Per-player load / unload ──────────────────────────────

    public void loadPlayer(UUID uuid) {
        File playerFile = new File(dataFolder, uuid.toString() + ".yml");
        PlayerData data = new PlayerData(uuid);

        if (playerFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            data.setFiber(config.getDouble("fiber", 0));
            data.setXp(config.getDouble("xp", 0));
            data.setLevel(config.getInt("level", 1));
            data.setFiberAmountUpgrade(config.getInt("upgrades.fiber-amount", 0));
            data.setMaterialAmountUpgrade(config.getInt("upgrades.material-amount", 0));
            data.setMaterialChanceUpgrade(config.getInt("upgrades.material-chance", 0));
            data.setCropCooldownUpgrade(config.getInt("upgrades.crop-cooldown", 0));
            data.setBonusFiberMultiplier(config.getDouble("bonus.fiber-multiplier", 0));
            data.setBonusMaterialAmountMultiplier(config.getDouble("bonus.material-amount-multiplier", 0));
            data.setBonusMaterialChanceMultiplier(config.getDouble("bonus.material-chance-multiplier", 0));
            data.setDriftwood(config.getDouble("materials.driftwood", 0));
            data.setMoss(config.getDouble("materials.moss", 0));
            data.setReed(config.getDouble("materials.reed", 0));
            data.setClover(config.getDouble("materials.clover", 0));
            data.setCompletedResearches(config.getInt("research.completed", 0));
            data.setActiveResearchIndex(config.getInt("research.active-index", -1));
            data.setActiveResearchStart(config.getLong("research.active-start", 0));
            data.setElderFiberLevel(config.getInt("elder.fiber-level", 0));
            data.setElderMaterialAmountLevel(config.getInt("elder.material-amount-level", 0));
            data.setElderXpGainLevel(config.getInt("elder.xp-gain-level", 0));
            data.setElderMaterialChanceLevel(config.getInt("elder.material-chance-level", 0));

            String petName = config.getString("pet.rarity", "NONE");
            try {
                data.setPetRarity(PetRarity.valueOf(petName));
            } catch (IllegalArgumentException e) {
                data.setPetRarity(PetRarity.NONE);
            }
        }

        playerDataMap.put(uuid, data);
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = playerDataMap.remove(uuid);
        if (data == null) return;

        // Save to disk asynchronously when they quit
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveToFile(data));
    }

    // ── Internal write helpers ────────────────────────────────

    private void saveToFile(PlayerData data) {
        File playerFile = new File(dataFolder, data.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("fiber", data.getFiber());
        config.set("xp", data.getXp());
        config.set("level", data.getLevel());
        config.set("upgrades.fiber-amount", data.getFiberAmountUpgrade());
        config.set("upgrades.material-amount", data.getMaterialAmountUpgrade());
        config.set("upgrades.material-chance", data.getMaterialChanceUpgrade());
        config.set("upgrades.crop-cooldown", data.getCropCooldownUpgrade());
        config.set("bonus.fiber-multiplier", data.getBonusFiberMultiplier());
        config.set("bonus.material-amount-multiplier", data.getBonusMaterialAmountMultiplier());
        config.set("bonus.material-chance-multiplier", data.getBonusMaterialChanceMultiplier());
        config.set("materials.driftwood", data.getDriftwood());
        config.set("materials.moss", data.getMoss());
        config.set("materials.reed", data.getReed());
        config.set("materials.clover", data.getClover());
        config.set("research.completed", data.getCompletedResearches());
        config.set("research.active-index", data.getActiveResearchIndex());
        config.set("research.active-start", data.getActiveResearchStart());
        config.set("elder.fiber-level", data.getElderFiberLevel());
        config.set("elder.material-amount-level", data.getElderMaterialAmountLevel());
        config.set("elder.xp-gain-level", data.getElderXpGainLevel());
        config.set("elder.material-chance-level", data.getElderMaterialChanceLevel());
        config.set("pet.rarity", data.getPetRarity().name());

        try {
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data for " + data.getUuid() + ": " + e.getMessage());
        }
    }

    // ── Public save API ───────────────────────────────────────

    // Synchronous save (used on plugin disable)
    public void saveAll() {
        for (PlayerData data : playerDataMap.values()) {
            saveToFile(data);
        }
    }

    public void savePlayerAsync(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> saveToFile(data));
        }
    }

    // Asynchronous save (used for the auto-save task)
    public void saveAllAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            for (PlayerData data : playerDataMap.values()) {
                saveToFile(data);
            }
        });
    }

    // ── Accessors ─────────────────────────────────────────────
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataMap.computeIfAbsent(uuid, PlayerData::new);
    }

    public boolean hasPlayerData(UUID uuid) {
        return playerDataMap.containsKey(uuid);
    }
}