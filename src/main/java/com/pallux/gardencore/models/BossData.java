package com.pallux.gardencore.models;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.*;

public class BossData {

    private final String key;
    private final String displayName;
    private final double maxHealth;
    private double currentHealth;
    private final int durationSeconds;
    private final String islandKey;
    private final String skullTexture;
    private final double size;
    private final String rewardCommand;
    private final String hologramFormat;

    // Runtime state
    private ArmorStand bodyStand;
    private ArmorStand nameStand;
    private final Map<UUID, Double> damageMap = new LinkedHashMap<>();
    private boolean active = false;
    private long spawnedAt = 0;

    // Zone
    private String worldName;
    private double minX, minY, minZ, maxX, maxY, maxZ;
    private boolean zoneSet = false;

    public BossData(String key, String displayName, double maxHealth, int durationSeconds,
                    String islandKey, String skullTexture, double size, String rewardCommand,
                    String hologramFormat) {
        this.key = key;
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.durationSeconds = durationSeconds;
        this.islandKey = islandKey;
        this.skullTexture = skullTexture;
        this.size = size;
        this.rewardCommand = rewardCommand;
        this.hologramFormat = hologramFormat != null ? hologramFormat
                : "{boss}\n&#FF6B6B❤ &7{hp} &8/ {max_hp}";
    }

    // ── Getters ────────────────────────────────────────────────

    public String getKey()              { return key; }
    public String getDisplayName()      { return displayName; }
    public double getMaxHealth()        { return maxHealth; }
    public double getCurrentHealth()    { return currentHealth; }
    public int getDurationSeconds()     { return durationSeconds; }
    public String getIslandKey()        { return islandKey; }
    public String getSkullTexture()     { return skullTexture; }
    public double getSize()             { return size; }
    public String getRewardCommand()    { return rewardCommand; }
    public String getHologramFormat()   { return hologramFormat; }
    public boolean isActive()           { return active; }
    public long getSpawnedAt()          { return spawnedAt; }
    public ArmorStand getBodyStand()    { return bodyStand; }
    public ArmorStand getNameStand()    { return nameStand; }
    public Map<UUID, Double> getDamageMap() { return damageMap; }

    // Zone
    public String getWorldName()  { return worldName; }
    public double getMinX()       { return minX; }
    public double getMinY()       { return minY; }
    public double getMinZ()       { return minZ; }
    public double getMaxX()       { return maxX; }
    public double getMaxY()       { return maxY; }
    public double getMaxZ()       { return maxZ; }
    public boolean isZoneSet()    { return zoneSet; }

    // ── Setters ────────────────────────────────────────────────

    public void setCurrentHealth(double h) { this.currentHealth = Math.max(0, h); }
    public void setActive(boolean active)  { this.active = active; }
    public void setSpawnedAt(long t)       { this.spawnedAt = t; }
    public void setBodyStand(ArmorStand s) { this.bodyStand = s; }
    public void setNameStand(ArmorStand s) { this.nameStand = s; }

    public void setZone(String world, double x1, double y1, double z1,
                        double x2, double y2, double z2) {
        this.worldName = world;
        this.minX = Math.min(x1, x2); this.maxX = Math.max(x1, x2);
        this.minY = Math.min(y1, y2); this.maxY = Math.max(y1, y2);
        this.minZ = Math.min(z1, z2); this.maxZ = Math.max(z1, z2);
        this.zoneSet = true;
    }

    public void reset() {
        this.currentHealth = maxHealth;
        this.active = false;
        this.spawnedAt = 0;
        this.bodyStand = null;
        this.nameStand = null;
        this.damageMap.clear();
    }

    /** Returns the center of the zone as a spawn location (Y = minY). */
    public Location getSpawnLocation() {
        if (!zoneSet) return null;
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(worldName);
        if (w == null) return null;
        double cx = (minX + maxX) / 2.0;
        double cy = minY;
        double cz = (minZ + maxZ) / 2.0;
        return new Location(w, cx, cy, cz);
    }
}