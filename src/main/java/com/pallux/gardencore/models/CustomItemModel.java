package com.pallux.gardencore.models;

import org.bukkit.Material;

import java.util.List;

public class CustomItemModel {

    private final String key;
    private final String name;
    private final Material material;
    private final List<String> lore;
    private final String command;

    public CustomItemModel(String key, String name, Material material, List<String> lore, String command) {
        this.key = key;
        this.name = name;
        this.material = material;
        this.lore = lore;
        this.command = command;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public List<String> getLore() { return lore; }
    public String getCommand() { return command; }
}