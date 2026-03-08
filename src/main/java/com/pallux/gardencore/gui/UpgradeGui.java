package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.managers.UpgradeManager;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.MessageUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class UpgradeGui implements Listener {

    private final GardenCore plugin;
    private final Player player;
    private final Inventory inventory;
    private final Map<Integer, UpgradeManager.UpgradeType> slotMap = new HashMap<>();

    public UpgradeGui(GardenCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        ConfigurationSection cfg = plugin.getConfigManager().getGuis().getConfigurationSection("upgrade-gui");
        String title = cfg != null ? ColorUtil.translate(cfg.getString("title", "&lUpgrades")) : "&lUpgrades";
        int size = cfg != null ? cfg.getInt("size", 36) : 36;

        this.inventory = Bukkit.createInventory(null, size, title);
        build();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        ConfigurationSection cfg = plugin.getConfigManager().getGuis().getConfigurationSection("upgrade-gui");
        if (cfg == null) return;

        slotMap.clear();

        boolean fillerEnabled = cfg.getBoolean("filler.enabled", true);
        if (fillerEnabled) {
            Material fillerMat = parseMaterial(cfg.getString("filler.material"), Material.GRAY_STAINED_GLASS_PANE);
            ItemStack filler = GuiUtil.createFiller(fillerMat);
            for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);
        }

        ConfigurationSection upgradesSec = cfg.getConfigurationSection("upgrades");
        if (upgradesSec == null) return;

        for (String key : upgradesSec.getKeys(false)) {
            UpgradeManager.UpgradeType type = UpgradeManager.fromString(key);
            if (type == null) continue;

            ConfigurationSection upSec = upgradesSec.getConfigurationSection(key);
            if (upSec == null) continue;

            int slot = upSec.getInt("slot", 0);
            Material mat = parseMaterial(upSec.getString("material"), Material.PAPER);
            String name = upSec.getString("name", "&7" + key);
            List<String> lore = upSec.getStringList("lore");

            int currentLevel = plugin.getUpgradeManager().getCurrentLevel(player, type);
            int maxLevel = plugin.getUpgradeManager().getMaxLevel(type);
            double cost = plugin.getUpgradeManager().getUpgradeCost(type, currentLevel);
            String value = resolveValue(type, currentLevel);

            String resolvedName = resolvePlaceholders(name, currentLevel, maxLevel, cost, value);
            List<String> resolvedLore = lore.stream()
                    .map(line -> resolvePlaceholders(line, currentLevel, maxLevel, cost, value))
                    .collect(Collectors.toList());

            ItemStack item = GuiUtil.createItem(mat, resolvedName, resolvedLore);
            inventory.setItem(slot, item);
            slotMap.put(slot, type);
        }
    }

    private String resolveValue(UpgradeManager.UpgradeType type, int currentLevel) {
        UUID uuid = player.getUniqueId();
        return switch (type) {
            case FIBER_AMOUNT -> NumberUtil.formatMultiplier(plugin.getMultiplierManager().getTotalFiberMultiplier(uuid));
            case MATERIAL_AMOUNT -> NumberUtil.formatMultiplier(plugin.getMultiplierManager().getTotalMaterialAmountMultiplier(uuid));
            case MATERIAL_CHANCE -> NumberUtil.formatMultiplier(plugin.getMultiplierManager().getTotalMaterialChanceMultiplier(uuid));
            case CROP_COOLDOWN -> NumberUtil.formatRaw(plugin.getUpgradeManager().getEffectiveCropCooldown(player));
        };
    }

    private String resolvePlaceholders(String text, int level, int maxLevel, double cost, String value) {
        return ColorUtil.translate(text
                .replace("{level}", String.valueOf(level))
                .replace("{max_level}", String.valueOf(maxLevel))
                .replace("{cost}", NumberUtil.formatRaw(cost))
                .replace("{value}", value));
    }

    private Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : fallback;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        if (!(event.getWhoClicked() instanceof Player clicker) || !clicker.equals(player)) return;

        event.setCancelled(true);

        UpgradeManager.UpgradeType type = slotMap.get(event.getRawSlot());
        if (type == null) return;

        UpgradeManager.UpgradeResult result = plugin.getUpgradeManager().tryUpgrade(player, type);

        switch (result) {
            case MAX_REACHED -> {
                player.closeInventory();
                MessageUtil.send(player, "upgrades.max-reached");
            }
            case NOT_ENOUGH_FIBER -> {
                player.closeInventory();
                MessageUtil.send(player, "upgrades.not-enough-fiber");
            }
            case SUCCESS -> {
                plugin.getDataManager().saveAsync();
                build();
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}