package com.pallux.gardencore.config;

import com.pallux.gardencore.GardenCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final GardenCore plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration crops;
    private FileConfiguration materials;
    private FileConfiguration events;
    private FileConfiguration aliases;
    private FileConfiguration guis;
    private FileConfiguration items;
    private FileConfiguration afkZone;
    private FileConfiguration research;
    private FileConfiguration gardenMenu;
    private FileConfiguration islandMenu;
    private FileConfiguration elder;
    private FileConfiguration pets;

    public ConfigManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        messages    = loadOrCreate("lang/messages.yml");
        crops       = loadOrCreate("settings/crops.yml");
        materials   = loadOrCreate("settings/materials.yml");
        events      = loadOrCreate("settings/events.yml");
        aliases     = loadOrCreate("settings/aliascommands.yml");
        items       = loadOrCreate("settings/items.yml");
        afkZone     = loadOrCreate("settings/afkzone.yml");
        pets        = loadOrCreate("settings/pets.yml");
        guis        = loadOrCreate("guis/upgradesmenu.yml");
        research    = loadOrCreate("guis/researchmenu.yml");
        gardenMenu  = loadOrCreate("guis/gardenmenu.yml");
        islandMenu  = loadOrCreate("guis/islandmenu.yml");
        elder       = loadOrCreate("guis/eldermenu.yml");
    }

    private FileConfiguration loadOrCreate(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(name, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reloadAll() {
        plugin.reloadConfig();
        config = plugin.getConfig();

        messages    = loadOrCreate("lang/messages.yml");
        crops       = loadOrCreate("settings/crops.yml");
        materials   = loadOrCreate("settings/materials.yml");
        events      = loadOrCreate("settings/events.yml");
        aliases     = loadOrCreate("settings/aliascommands.yml");
        items       = loadOrCreate("settings/items.yml");
        afkZone     = loadOrCreate("settings/afkzone.yml");
        pets        = loadOrCreate("settings/pets.yml");
        guis        = loadOrCreate("guis/upgradesmenu.yml");
        research    = loadOrCreate("guis/researchmenu.yml");
        gardenMenu  = loadOrCreate("guis/gardenmenu.yml");
        islandMenu  = loadOrCreate("guis/islandmenu.yml");
        elder       = loadOrCreate("guis/eldermenu.yml");
    }

    public boolean isFeatureEnabled(String feature) {
        return config.getBoolean("features." + feature, true);
    }

    public String getPrefix() {
        return config.getString("prefix", "&8[&a&lGardenCore&8]&r");
    }

    public String getMessage(String path) {
        String msg = messages.getString(path);
        return msg != null ? msg : "&cMessage not found: " + path;
    }

    public int getEventInterval() {
        return config.getInt("events.interval-minutes", 30);
    }

    public int getEventDuration() {
        return config.getInt("events.duration-minutes", 5);
    }

    public int getLevelMilestone() {
        return config.getInt("broadcast.level-milestone", 10);
    }

    public FileConfiguration getConfig()         { return config; }
    public FileConfiguration getMessages()       { return messages; }
    public FileConfiguration getCrops()          { return crops; }
    public FileConfiguration getMaterials()      { return materials; }
    public FileConfiguration getEventsConfig()   { return events; }
    public FileConfiguration getAliases()        { return aliases; }
    public FileConfiguration getGuis()           { return guis; }
    public FileConfiguration getItems()          { return items; }
    public FileConfiguration getAfkZone()        { return afkZone; }
    public FileConfiguration getResearchConfig() { return research; }
    public FileConfiguration getGardenMenu()     { return gardenMenu; }
    public FileConfiguration getIslandMenu()     { return islandMenu; }
    public FileConfiguration getElderConfig()    { return elder; }
    public FileConfiguration getPetsConfig()     { return pets; }
}