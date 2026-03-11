package com.pallux.gardencore;

import com.pallux.gardencore.commands.*;
import com.pallux.gardencore.config.ConfigManager;
import com.pallux.gardencore.data.DataManager;
import com.pallux.gardencore.events.EventManager;
import com.pallux.gardencore.gui.ElderGui;
import com.pallux.gardencore.gui.ResearchGui;
import com.pallux.gardencore.hooks.PlaceholderHook;
import com.pallux.gardencore.listeners.*;
import com.pallux.gardencore.managers.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GardenCore extends JavaPlugin {

    private static GardenCore instance;

    private ConfigManager configManager;
    private DataManager dataManager;
    private FiberManager fiberManager;
    private LevelManager levelManager;
    private CropManager cropManager;
    private MaterialManager materialManager;
    private MultiplierManager multiplierManager;
    private UpgradeManager upgradeManager;
    private EventManager eventManager;
    private AliasManager aliasManager;
    private ItemManager itemManager;
    private AfkZoneManager afkZoneManager;
    private ResearchManager researchManager;
    private ResearchGui researchGui;
    private ElderManager elderManager;
    private ElderGui elderGui;
    private PetManager petManager;
    private PetCosmeticManager petCosmeticManager;
    private ComposterManager composterManager;
    private final Map<UUID, Integer> researchPageMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadAll();

        dataManager = new DataManager(this);
        dataManager.load();

        fiberManager = new FiberManager(this);
        levelManager = new LevelManager(this);
        cropManager = new CropManager(this);
        materialManager = new MaterialManager(this);
        multiplierManager = new MultiplierManager(this);
        upgradeManager = new UpgradeManager(this);
        eventManager = new EventManager(this);
        aliasManager = new AliasManager(this);
        itemManager = new ItemManager(this);
        afkZoneManager = new AfkZoneManager(this);
        researchManager = new ResearchManager(this);
        researchGui = new ResearchGui(this);
        elderManager = new ElderManager(this);
        elderGui = new ElderGui(this);
        petManager = new PetManager(this);
        petCosmeticManager = new PetCosmeticManager(this);
        composterManager = new ComposterManager(this);

        registerListeners();
        registerCommands();
        registerPlaceholders();

        aliasManager.registerAliases();

        getLogger().info("GardenCore has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (petCosmeticManager != null) {
            petCosmeticManager.shutdown();
        }
        if (composterManager != null) {
            composterManager.shutdown();
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (afkZoneManager != null) {
            afkZoneManager.shutdown();
        }
        if (researchManager != null) {
            researchManager.shutdown();
        }
        getLogger().info("GardenCore has been disabled.");
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();

        if (configManager.isFeatureEnabled("no-drop")) {
            pm.registerEvents(new NoDropListener(this), this);
        }
        if (configManager.isFeatureEnabled("no-fall-damage")) {
            pm.registerEvents(new NoFallDamageListener(this), this);
        }
        if (configManager.isFeatureEnabled("no-hunger")) {
            pm.registerEvents(new NoHungerListener(this), this);
        }
        if (configManager.isFeatureEnabled("no-block-drops")) {
            pm.registerEvents(new NoBlockDropsListener(this), this);
        }
        if (configManager.isFeatureEnabled("no-non-crop-break")) {
            pm.registerEvents(new NoNonCropBreakListener(this), this);
        }
        if (configManager.isFeatureEnabled("instant-replant")) {
            pm.registerEvents(new InstantReplantListener(this), this);
        }
        if (configManager.isFeatureEnabled("crop-farming")) {
            pm.registerEvents(new CropFarmingListener(this), this);
        }
        if (configManager.isFeatureEnabled("command-blocking")) {
            pm.registerEvents(new BlockedCommandListener(this), this);
        }
        if (configManager.isFeatureEnabled("composters")) {
            pm.registerEvents(new ComposterListener(this), this);
        }

        pm.registerEvents(new PlayerConnectionListener(this), this);
        pm.registerEvents(new CustomItemListener(this), this);
        pm.registerEvents(new AfkZoneListener(this), this);
        pm.registerEvents(new ResearchListener(this), this);
        pm.registerEvents(new ElderListener(this), this);
    }

    private void registerCommands() {
        var upgradeCmd = getCommand("upgrade");
        if (upgradeCmd != null) {
            var cmd = new UpgradeCommand(this);
            upgradeCmd.setExecutor(cmd);
            upgradeCmd.setTabCompleter(cmd);
        }

        var gcaCmd = getCommand("gca");
        if (gcaCmd != null) {
            var cmd = new AdminCommand(this);
            gcaCmd.setExecutor(cmd);
            gcaCmd.setTabCompleter(cmd);
        }

        var researchCmd = getCommand("research");
        if (researchCmd != null) {
            researchCmd.setExecutor(new ResearchCommand(this));
        }

        var gardenCmd = getCommand("garden");
        if (gardenCmd != null) {
            gardenCmd.setExecutor(new GardenCommand(this));
        }

        var islandCmd = getCommand("islands");
        if (islandCmd != null) {
            islandCmd.setExecutor(new IslandCommand(this));
        }

        var elderCmd = getCommand("elder");
        if (elderCmd != null) {
            elderCmd.setExecutor(new ElderCommand(this));
        }

        var petsCmd = getCommand("pets");
        if (petsCmd != null) {
            petsCmd.setExecutor(new PetsCommand(this));
        }
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            getLogger().info("PlaceholderAPI hooked successfully.");
        }
    }

    public static GardenCore getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager()                   { return configManager; }
    public DataManager getDataManager()                       { return dataManager; }
    public FiberManager getFiberManager()                     { return fiberManager; }
    public LevelManager getLevelManager()                     { return levelManager; }
    public CropManager getCropManager()                       { return cropManager; }
    public MaterialManager getMaterialManager()               { return materialManager; }
    public MultiplierManager getMultiplierManager()           { return multiplierManager; }
    public UpgradeManager getUpgradeManager()                 { return upgradeManager; }
    public EventManager getEventManager()                     { return eventManager; }
    public AliasManager getAliasManager()                     { return aliasManager; }
    public ItemManager getItemManager()                       { return itemManager; }
    public AfkZoneManager getAfkZoneManager()                 { return afkZoneManager; }
    public ResearchManager getResearchManager()               { return researchManager; }
    public ResearchGui getResearchGui()                       { return researchGui; }
    public ElderManager getElderManager()                     { return elderManager; }
    public ElderGui getElderGui()                             { return elderGui; }
    public PetManager getPetManager()                         { return petManager; }
    public PetCosmeticManager getPetCosmeticManager()         { return petCosmeticManager; }
    public ComposterManager getComposterManager()             { return composterManager; }

    public int getPlayerResearchPage(Player player) {
        return researchPageMap.getOrDefault(player.getUniqueId(), 0);
    }

    public void setPlayerResearchPage(Player player, int page) {
        researchPageMap.put(player.getUniqueId(), page);
    }
}