package com.pallux.gardencore.gui;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.MessageUtil;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IslandMenuGui implements Listener {

    private final GardenCore plugin;
    private final Player player;
    private final Inventory inventory;

    private final Map<Integer, IslandButton> buttonMap = new HashMap<>();
    private int slotClose = -1;

    private record IslandButton(String command, int levelRequired) {}

    public IslandMenuGui(GardenCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;

        FileConfiguration cfg = plugin.getConfigManager().getIslandMenu();
        ConfigurationSection sec = cfg.getConfigurationSection("island-menu");

        String title = sec != null
                ? ColorUtil.translate(sec.getString("title", "&8Island Menu"))
                : ColorUtil.translate("&8Island Menu");
        int size = sec != null ? sec.getInt("size", 27) : 27;

        this.inventory = Bukkit.createInventory(null, size, title);
        build();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void build() {
        FileConfiguration cfg = plugin.getConfigManager().getIslandMenu();
        ConfigurationSection sec = cfg.getConfigurationSection("island-menu");
        if (sec == null) return;

        buttonMap.clear();

        // Filler
        Material fillerMat = parseMaterial(
                sec.getString("filler.material", "GRAY_STAINED_GLASS_PANE"),
                Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = GuiUtil.createFiller(fillerMat);
        for (int i = 0; i < inventory.getSize(); i++) inventory.setItem(i, filler);

        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());

        // Island buttons
        ConfigurationSection buttonsSec = sec.getConfigurationSection("islands");
        if (buttonsSec != null) {
            for (String key : buttonsSec.getKeys(false)) {
                ConfigurationSection btnSec = buttonsSec.getConfigurationSection(key);
                if (btnSec == null) continue;

                int slot = btnSec.getInt("slot", -1);
                if (slot < 0 || slot >= inventory.getSize()) continue;

                int levelRequired = btnSec.getInt("level-required", 1);
                String command = btnSec.getString("command", "").trim();
                boolean unlocked = playerLevel >= levelRequired;

                ItemStack item = unlocked
                        ? buildUnlockedButton(btnSec, levelRequired)
                        : buildLockedButton(btnSec, levelRequired, sec);

                inventory.setItem(slot, item);
                buttonMap.put(slot, new IslandButton(unlocked ? command : "", levelRequired));
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
            slotClose = closeSec.getInt("slot", -1);
            if (slotClose >= 0 && slotClose < inventory.getSize()) {
                Material mat = parseMaterial(closeSec.getString("material"), Material.BARRIER);
                String name = closeSec.getString("name", "&#ff7a7a&lClose");
                List<String> lore = closeSec.getStringList("lore").stream()
                        .map(ColorUtil::translate).collect(Collectors.toList());
                inventory.setItem(slotClose, GuiUtil.createItem(mat, name, lore));
            }
        }
    }

    private ItemStack buildUnlockedButton(ConfigurationSection btnSec, int levelRequired) {
        Material mat = parseMaterial(btnSec.getString("material"), Material.GREEN_STAINED_GLASS_PANE);
        String name = ColorUtil.translate(btnSec.getString("name", "&7Island"));
        List<String> lore = btnSec.getStringList("lore").stream()
                .map(l -> ColorUtil.translate(l.replace("{level_required}", String.valueOf(levelRequired))))
                .collect(Collectors.toList());
        return GuiUtil.createItem(mat, name, lore);
    }

    private ItemStack buildLockedButton(ConfigurationSection btnSec, int levelRequired, ConfigurationSection rootSec) {
        String lockedMatName = btnSec.getString("locked-material",
                rootSec.getString("locked-material", "RED_STAINED_GLASS_PANE"));
        Material mat = parseMaterial(lockedMatName, Material.RED_STAINED_GLASS_PANE);

        String lockedNameTemplate = rootSec.getString("locked-name", "&#ff7a7a&l{name} &7[Locked]");
        String islandName = ColorUtil.strip(ColorUtil.translate(btnSec.getString("name", "Island")));
        String name = ColorUtil.translate(lockedNameTemplate.replace("{name}", islandName));

        List<String> lockedLore = rootSec.getStringList("locked-lore").stream()
                .map(l -> ColorUtil.translate(l.replace("{level_required}", String.valueOf(levelRequired))))
                .collect(Collectors.toList());

        return GuiUtil.createItem(mat, name, lockedLore);
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

        if (slot == slotClose) {
            player.closeInventory();
            return;
        }

        IslandButton button = buttonMap.get(slot);
        if (button == null) return;

        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());

        if (playerLevel < button.levelRequired()) {
            player.closeInventory();
            MessageUtil.send(player, "islands.level-required",
                    Map.of("level", String.valueOf(button.levelRequired())));
            return;
        }

        if (button.command().isEmpty()) return;

        player.closeInventory();
        final String cmd = button.command().replace("{player}", player.getName());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> player.performCommand(cmd), 1L);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!event.getInventory().equals(inventory)) return;
        HandlerList.unregisterAll(this);
    }
}