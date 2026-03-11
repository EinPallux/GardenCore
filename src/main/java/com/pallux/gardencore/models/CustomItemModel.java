package com.pallux.gardencore.models;

import org.bukkit.Material;

import java.util.List;

public class CustomItemModel {

    private final String key;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final String command;       // empty for items with no right-click action
    private final int durationSeconds;  // > 0 for composter items, 0 for all others
    private final double radius;        // > 0 for composter items, 0 for all others
    private final List<String> hologramLines; // configurable hologram lines for composter items

    public CustomItemModel(String key, String name, Material material, List<String> lore,
                           String command, int durationSeconds, double radius,
                           List<String> hologramLines) {
        this.key             = key;
        this.name            = name;
        this.material        = material;
        this.lore            = lore;
        this.command         = command != null ? command : "";
        this.durationSeconds = durationSeconds;
        this.radius          = radius;
        this.hologramLines   = hologramLines != null ? hologramLines : List.of();
    }

    public String getKey()                  { return key; }
    public String getName()                 { return name; }
    public Material getMaterial()           { return material; }
    public List<String> getLore()           { return lore; }
    public String getCommand()              { return command; }
    public boolean hasCommand()             { return !command.isBlank(); }
    public int getDurationSeconds()         { return durationSeconds; }
    public double getRadius()               { return radius; }
    public List<String> getHologramLines()  { return hologramLines; }
    public boolean hasHologramLines()       { return !hologramLines.isEmpty(); }
}