package com.pallux.gardencore.models;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;

import java.util.ArrayList;
import java.util.List;

public class ComposterData {

    public enum ComposterType {
        FIBER_100    ("composter_fiber_100",     "+100% Fiber",           "&#FFD700", 100),
        FIBER_250    ("composter_fiber_250",     "+250% Fiber",           "&#FFD700", 250),
        XP_50        ("composter_xp_50",         "+50% XP",               "&#a8d8ff",  50),
        XP_75        ("composter_xp_75",         "+75% XP",               "&#a8d8ff",  75),
        MATERIAL_100 ("composter_material_100",  "+100% Material Amount", "&#a8ff78", 100),
        MATERIAL_150 ("composter_material_150",  "+150% Material Amount", "&#a8ff78", 150);

        private final String itemKey;
        private final String displayName;
        private final String displayColor;
        private final double bonusPercent;

        ComposterType(String itemKey, String displayName, String displayColor, double bonusPercent) {
            this.itemKey       = itemKey;
            this.displayName   = displayName;
            this.displayColor  = displayColor;
            this.bonusPercent  = bonusPercent;
        }

        public String getItemKey()       { return itemKey; }
        public String getDisplayName()   { return displayName; }
        public String getDisplayColor()  { return displayColor; }
        public double getBonusPercent()  { return bonusPercent; }

        public static ComposterType fromItemKey(String key) {
            for (ComposterType t : values()) {
                if (t.itemKey.equalsIgnoreCase(key)) return t;
            }
            return null;
        }
    }

    // ── Instance fields ───────────────────────────────────────

    private final Location blockLocation;
    private final String placerName;
    private final ComposterType type;
    private final long placedAtMillis;
    private final int durationSeconds;
    private final double radius;

    private List<ArmorStand> hologramStands = new ArrayList<>();

    public ComposterData(Location blockLocation, String placerName, ComposterType type,
                         long placedAtMillis, int durationSeconds, double radius) {
        this.blockLocation  = blockLocation;
        this.placerName     = placerName;
        this.type           = type;
        this.placedAtMillis = placedAtMillis;
        this.durationSeconds = durationSeconds;
        this.radius         = radius;
    }

    public Location getBlockLocation()      { return blockLocation; }
    public String getPlacerName()           { return placerName; }
    public ComposterType getType()          { return type; }
    public int getDurationSeconds()         { return durationSeconds; }
    public double getRadius()               { return radius; }

    public long getRemainingSeconds() {
        long elapsed = (System.currentTimeMillis() - placedAtMillis) / 1000L;
        return Math.max(0, durationSeconds - elapsed);
    }

    public List<ArmorStand> getHologramStands()                    { return hologramStands; }
    public void setHologramStands(List<ArmorStand> hologramStands) { this.hologramStands = hologramStands; }
}