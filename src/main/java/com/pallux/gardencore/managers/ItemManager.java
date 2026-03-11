package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.CustomItemModel;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class ItemManager {

    private static final String NBT_KEY = "gc_item_key";

    private final GardenCore plugin;
    private final NamespacedKey namespacedKey;
    private final Map<String, CustomItemModel> items = new LinkedHashMap<>();

    public ItemManager(GardenCore plugin) {
        this.plugin = plugin;
        this.namespacedKey = new NamespacedKey(plugin, NBT_KEY);
        loadItems();
    }

    public void loadItems() {
        items.clear();
        ConfigurationSection section = plugin.getConfigManager().getItems().getConfigurationSection("items");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String name     = section.getString(key + ".name", key);
            String matName  = section.getString(key + ".material", "PAPER");
            Material material = Material.matchMaterial(matName);
            if (material == null) {
                plugin.getLogger().warning("Unknown material '" + matName + "' for item '" + key + "' in items.yml");
                material = Material.PAPER;
            }
            List<String> lore       = section.getStringList(key + ".lore");
            String command          = section.getString(key + ".command", "");
            int    durationSeconds  = section.getInt(key + ".duration-seconds", 0);
            double radius           = section.getDouble(key + ".radius", 0.0);

            items.put(key, new CustomItemModel(key, name, material, lore, command, durationSeconds, radius));
        }

        plugin.getLogger().info("Loaded " + items.size() + " custom items from items.yml");
    }

    public ItemStack buildItem(String key, int amount) {
        CustomItemModel model = items.get(key);
        if (model == null) return null;

        ItemStack stack = new ItemStack(model.getMaterial(), amount);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        meta.setDisplayName(ColorUtil.translate(model.getName()));
        meta.setLore(model.getLore().stream().map(ColorUtil::translate).collect(Collectors.toList()));

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(namespacedKey, PersistentDataType.STRING, key);

        stack.setItemMeta(meta);
        return stack;
    }

    public void giveItem(Player player, String key, int amount) {
        ItemStack stack = buildItem(key, amount);
        if (stack == null) return;
        player.getInventory().addItem(stack).values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    public String getItemKey(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    /** Returns the active duration in seconds for a composter item, or 0 if not set. */
    public int getDurationSeconds(String key) {
        CustomItemModel model = items.get(key);
        return model != null ? model.getDurationSeconds() : 0;
    }

    /** Returns the buff radius for a composter item, or 0 if not set. */
    public double getRadius(String key) {
        CustomItemModel model = items.get(key);
        return model != null ? model.getRadius() : 0.0;
    }

    /**
     * Executes the right-click command for a custom item.
     * Composter items are excluded upstream in CustomItemListener and never reach here.
     */
    public void executeItem(Player player, ItemStack stack) {
        String key = getItemKey(stack);
        if (key == null) return;

        CustomItemModel model = items.get(key);
        if (model == null) return;

        if (!model.hasCommand()) return;

        String cmd = model.getCommand().replace("%player%", player.getName()).trim();

        if (cmd.toLowerCase().startsWith("gca event start ")) {
            String eventKey = cmd.substring("gca event start ".length()).trim();
            boolean started = plugin.getEventManager().startEventByKey(eventKey, true);
            if (!started) {
                plugin.getLogger().warning("Item '" + key + "' tried to start unknown event: " + eventKey);
            }
        } else {
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
        }

        if (stack.getAmount() > 1) {
            stack.setAmount(stack.getAmount() - 1);
        } else {
            player.getInventory().remove(stack);
        }
    }

    public Optional<CustomItemModel> getItem(String key) {
        return Optional.ofNullable(items.get(key));
    }

    public boolean isCustomItem(ItemStack stack) {
        return getItemKey(stack) != null;
    }

    public Set<String> getItemKeys() {
        return Collections.unmodifiableSet(items.keySet());
    }
}