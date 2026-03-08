package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PlayerData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class GardenMenuGui implements Listener {

    private final GardenCore plugin;
    private final Player player;
    private final Inventory inventory;

    // slot -> command string (without leading /)
    private final Map<Integer, String> buttonCommands = new HashMap<>();
    private int slotClose = 49;

    public GardenMenuGui(GardenCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration cfg = plugin.getConfigManager().getGardenMenu();
        ConfigurationSection sec = cfg.getConfigurationSection("garden-menu");

        String title = sec != null
                ? ColorUtil.translate(sec.getString("title", "&8Garden Menu"))
                : ColorUtil.translate("&8Garden Menu");
        int size = sec != null ? sec.getInt("size", 54) : 54;

        this.inventory = Bukkit.createInventory(null, size, title);
        build();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        FileConfiguration cfg = plugin.getConfigManager().getGardenMenu();
        ConfigurationSection sec = cfg.getConfigurationSection("garden-menu");
        if (sec == null) return;

        buttonCommands.clear();

        // Gray filler
        Material fillerMat = parseMaterial(
                sec.getString("filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = GuiUtil.createFiller(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        // Player head
        ConfigurationSection headSec = sec.getConfigurationSection("player-head");
        if (headSec != null) {
            int headSlot = headSec.getInt("slot", 4);
            String headName = headSec.getString("name", "&#a8ff78&l{player}'s Stats");
            List<String> headLore = headSec.getStringList("lore");
            inventory.setItem(headSlot, buildPlayerHead(headName, headLore));
        }

        // Feature buttons — generic: iterate all keys, read slot + command + appearance
        ConfigurationSection buttonsSec = sec.getConfigurationSection("buttons");
        if (buttonsSec != null) {
            for (String key : buttonsSec.getKeys(false)) {
                ConfigurationSection btnSec = buttonsSec.getConfigurationSection(key);
                if (btnSec == null) continue;

                int slot = btnSec.getInt("slot", -1);
                if (slot < 0 || slot >= inventory.getSize()) continue;

                String command = btnSec.getString("command", "").trim();
                if (!command.isEmpty()) {
                    buttonCommands.put(slot, command);
                }

                inventory.setItem(slot, buildSimpleButton(btnSec));
            }
        }

        // Placeholder items
        ConfigurationSection placeholderItem = sec.getConfigurationSection("placeholder-item");
        Material placeholderMat = parseMaterial(
                placeholderItem != null ? placeholderItem.getString("material") : "GRAY_DYE",
                Material.GRAY_DYE);
        String placeholderName = placeholderItem != null
                ? placeholderItem.getString("name", "&8Coming Soon") : "&8Coming Soon";
        List<String> placeholderLore = placeholderItem != null
                ? placeholderItem.getStringList("lore") : List.of();
        ItemStack placeholderStack = GuiUtil.createItem(placeholderMat, placeholderName, placeholderLore);

        List<?> placeholderSlots = sec.getList("placeholders");
        if (placeholderSlots != null) {
            for (Object entry : placeholderSlots) {
                if (entry instanceof Map<?, ?> map) {
                    Object slotVal = map.get("slot");
                    if (slotVal instanceof Integer s) {
                        inventory.setItem(s, placeholderStack);
                    }
                }
            }
        }

        // Close button
        ConfigurationSection closeSec = sec.getConfigurationSection("close-button");
        if (closeSec != null) {
            slotClose = closeSec.getInt("slot", 49);
            inventory.setItem(slotClose, buildSimpleButton(closeSec));
        }
    }

    private ItemStack buildPlayerHead(String nameTemplate, List<String> loreTemplate) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        List<String> lore = loreTemplate.stream()
                .map(line -> ColorUtil.translate(resolvePlaceholders(line, data)))
                .collect(Collectors.toList());

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(ColorUtil.translate(resolvePlaceholders(nameTemplate, data)));
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack buildSimpleButton(ConfigurationSection sec) {
        Material mat = parseMaterial(sec.getString("material"), Material.PAPER);
        String name = sec.getString("name", "&7Button");
        List<String> lore = sec.getStringList("lore").stream()
                .map(ColorUtil::translate)
                .collect(Collectors.toList());
        return GuiUtil.createItem(mat, name, lore);
    }

    private String resolvePlaceholders(String text, PlayerData data) {
        UUID uuid = player.getUniqueId();

        String researchActive;
        if (data.hasActiveResearch()) {
            long remainMs = plugin.getResearchManager().getTimeRemainingMs(data);
            String remaining = plugin.getResearchManager().formatDuration(remainMs / 1000);
            researchActive = plugin.getResearchManager().getResearchName(data.getActiveResearchIndex())
                    + " &7(" + remaining + ")";
        } else {
            researchActive = "&7None";
        }

        return text
                .replace("{player}", player.getName())
                .replace("{level}", String.valueOf(data.getLevel()))
                .replace("{xp}", NumberUtil.formatRaw(plugin.getLevelManager().getXpPercent(uuid)) + "%")
                .replace("{fiber}", NumberUtil.formatRaw(data.getFiber()))
                .replace("{fiber_raw}", NumberUtil.formatRaw(data.getFiber()))
                .replace("{multi_fiber}", plugin.getMultiplierManager().formatFiberMultiplier(uuid))
                .replace("{multi_material}", plugin.getMultiplierManager().formatMaterialAmountMultiplier(uuid))
                .replace("{multi_material_chance}", plugin.getMultiplierManager().formatMaterialChanceMultiplier(uuid))
                .replace("{upgrade_fiber}", String.valueOf(data.getFiberAmountUpgrade()))
                .replace("{upgrade_material}", String.valueOf(data.getMaterialAmountUpgrade()))
                .replace("{upgrade_material_chance}", String.valueOf(data.getMaterialChanceUpgrade()))
                .replace("{upgrade_cooldown}", String.valueOf(data.getCropCooldownUpgrade()))
                .replace("{research_completed}", String.valueOf(data.getCompletedResearches()))
                .replace("{research_active}", researchActive)
                .replace("{driftwood}", NumberUtil.formatRaw(data.getDriftwood()))
                .replace("{moss}", NumberUtil.formatRaw(data.getMoss()))
                .replace("{reed}", NumberUtil.formatRaw(data.getReed()))
                .replace("{clover}", NumberUtil.formatRaw(data.getClover()));
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

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        // Close button
        if (slot == slotClose) {
            player.closeInventory();
            return;
        }

        // Any configured button
        String command = buttonCommands.get(slot);
        if (command == null) return;

        player.closeInventory();

        // 1-tick delay so the inventory close processes before the command runs
        final String cmd = command.replace("{player}", player.getName());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> player.performCommand(cmd), 1L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}