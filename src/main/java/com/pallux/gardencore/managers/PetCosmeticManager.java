package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PetRarity;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Spawns a small, orbiting ArmorStand with a configurable item on its head
 * for every online player who owns a pet.
 *
 * All appearance is driven by settings/pets.yml under
 * pets.rarities.<key>.cosmetic:
 *   material:      Minecraft material name (default PLAYER_HEAD)
 *   skull-texture: Base64 skin value string (only used when material is PLAYER_HEAD)
 */
public class PetCosmeticManager {

    // ── Orbit parameters ──────────────────────────────────────
    private static final double ORBIT_RADIUS = 0.75;
    private static final double ORBIT_HEIGHT = 0.35;
    private static final double ORBIT_SPEED  = 0.05; // radians per tick

    private final GardenCore plugin;

    /** uuid → orbiting ArmorStand */
    private final Map<UUID, ArmorStand> stands = new ConcurrentHashMap<>();

    /** uuid → current orbit angle (radians) */
    private final Map<UUID, Double> angles = new ConcurrentHashMap<>();

    private BukkitTask orbitTask;

    public PetCosmeticManager(GardenCore plugin) {
        this.plugin = plugin;
        startOrbitTask();
    }

    // ── Lifecycle ──────────────────────────────────────────────

    /** Spawn or replace the cosmetic stand for this player. */
    public void refresh(Player player) {
        despawn(player.getUniqueId());

        PetRarity rarity = plugin.getPetManager().getPlayerPet(player.getUniqueId());
        if (rarity == PetRarity.NONE) return;

        ArmorStand stand = spawnStand(player, rarity);
        stands.put(player.getUniqueId(), stand);
        angles.putIfAbsent(player.getUniqueId(), 0.0);
    }

    /** Remove the cosmetic stand for this player. */
    public void despawn(UUID uuid) {
        ArmorStand stand = stands.remove(uuid);
        if (stand != null && stand.isValid()) stand.remove();
    }

    /** Despawn all stands and cancel the orbit task. */
    public void shutdown() {
        if (orbitTask != null) orbitTask.cancel();
        new HashSet<>(stands.keySet()).forEach(this::despawn);
    }

    /** Refresh all online players (called after /gca reload). */
    public void refreshAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    // ── Orbit task ────────────────────────────────────────────

    private void startOrbitTask() {
        orbitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Iterator<Map.Entry<UUID, ArmorStand>> it = stands.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, ArmorStand> entry = it.next();
                UUID uuid     = entry.getKey();
                ArmorStand stand = entry.getValue();

                if (!stand.isValid()) {
                    it.remove();
                    angles.remove(uuid);
                    continue;
                }

                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    stand.remove();
                    it.remove();
                    angles.remove(uuid);
                    continue;
                }

                double angle = angles.merge(uuid, ORBIT_SPEED, Double::sum);

                Location loc = player.getLocation();
                double x = loc.getX() + ORBIT_RADIUS * Math.cos(angle);
                double z = loc.getZ() + ORBIT_RADIUS * Math.sin(angle);
                double y = loc.getY() + player.getEyeHeight() + ORBIT_HEIGHT;

                stand.teleport(new Location(loc.getWorld(), x, y, z));
            }
        }, 1L, 1L);
    }

    // ── Stand spawning ────────────────────────────────────────

    private ArmorStand spawnStand(Player player, PetRarity rarity) {
        Location loc = player.getLocation().clone()
                .add(ORBIT_RADIUS, player.getEyeHeight() + ORBIT_HEIGHT, 0);

        return loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setSmall(true);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);

            for (org.bukkit.inventory.EquipmentSlot slot : org.bukkit.inventory.EquipmentSlot.values()) {
                stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
            }

            stand.getEquipment().setHelmet(buildHelmetItem(rarity));

            stand.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "pet_cosmetic"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    player.getUniqueId().toString()
            );
        });
    }

    // ── Item builder — reads entirely from pets.yml ────────────

    /**
     * Reads pets.rarities.<key>.cosmetic from config.
     *
     * If material is PLAYER_HEAD (or absent), uses skull-texture (Base64).
     * Any other material is used as a plain item with no display name.
     * Falls back to a plain PLAYER_HEAD if config is missing or invalid.
     */
    private ItemStack buildHelmetItem(PetRarity rarity) {
        ConfigurationSection sec = plugin.getConfigManager().getPetsConfig()
                .getConfigurationSection("pets.rarities." + rarity.getConfigKey() + ".cosmetic");

        String materialName  = sec != null ? sec.getString("material",      "PLAYER_HEAD") : "PLAYER_HEAD";
        String skullTexture  = sec != null ? sec.getString("skull-texture",  "")           : "";

        Material mat = Material.matchMaterial(materialName != null ? materialName : "");
        if (mat == null) mat = Material.PLAYER_HEAD;

        ItemStack item = new ItemStack(mat);

        if (mat == Material.PLAYER_HEAD && !skullTexture.isBlank()) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta != null) {
                applyTexture(meta, skullTexture.trim());
                meta.setDisplayName(" ");
                item.setItemMeta(meta);
            }
        } else {
            // Non-skull material — just blank the display name
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    /**
     * Decodes a Base64 skin value string and applies it to a SkullMeta
     * via the Paper PlayerProfile API.
     *
     * The value string is the standard Mojang texture value field —
     * a Base64-encoded JSON blob like:
     *   {"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/..."}}}
     */
    private void applyTexture(SkullMeta meta, String base64Value) {
        try {
            String decoded = new String(Base64.getDecoder().decode(base64Value));
            int start = decoded.indexOf("\"url\":\"") + 7;
            int end   = decoded.indexOf("\"", start);
            if (start < 7 || end <= start) return;

            String url = decoded.substring(start, end);

            org.bukkit.profile.PlayerProfile profile =
                    Bukkit.createPlayerProfile(UUID.randomUUID(), "PetCosmetic");
            org.bukkit.profile.PlayerTextures textures = profile.getTextures();
            textures.setSkin(new URL(url));
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "PetCosmeticManager: failed to apply skull texture for a pet rarity: " + e.getMessage());
        }
    }
}