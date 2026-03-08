package com.pallux.gardencore.models;

import org.bukkit.Material;

public class CropData {

    private final String key;
    private final String displayName;
    private final Material material;
    private final double fiber;
    private final double xp;
    private final int levelRequired;

    public CropData(String key, String displayName, Material material, double fiber, double xp, int levelRequired) {
        this.key = key;
        this.displayName = displayName;
        this.material = material;
        this.fiber = fiber;
        this.xp = xp;
        this.levelRequired = levelRequired;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material; }
    public double getFiber() { return fiber; }
    public double getXp() { return xp; }
    public int getLevelRequired() { return levelRequired; }
}
