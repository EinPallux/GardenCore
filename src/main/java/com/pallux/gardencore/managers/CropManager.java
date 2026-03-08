package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.CropData;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CropManager {

    private final GardenCore plugin;
    private final Map<Material, CropData> crops = new HashMap<>();

    public CropManager(GardenCore plugin) {
        this.plugin = plugin;
        loadCrops();
    }

    private void loadCrops() {
        crops.clear();
        ConfigurationSection section = plugin.getConfigManager().getCrops().getConfigurationSection("crops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String matName = section.getString(key + ".material", key);
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                plugin.getLogger().warning("Unknown material '" + matName + "' for crop '" + key + "' in crops.yml");
                continue;
            }

            String displayName = section.getString(key + ".display-name", key);
            double fiber = section.getDouble(key + ".fiber", 1.0);
            double xp = section.getDouble(key + ".xp", 5.0);
            int levelRequired = section.getInt(key + ".level-required", 1);

            crops.put(material, new CropData(key, displayName, material, fiber, xp, levelRequired));
        }

        plugin.getLogger().info("Loaded " + crops.size() + " crops from crops.yml");
    }

    public Optional<CropData> getCropData(Material material) {
        return Optional.ofNullable(crops.get(material));
    }

    public boolean isCrop(Material material) {
        return crops.containsKey(material);
    }

    public Map<Material, CropData> getAllCrops() {
        return Map.copyOf(crops);
    }
}
