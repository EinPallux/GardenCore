package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.ComposterData;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ComposterManager {

    private final GardenCore plugin;
    private final Map<String, ComposterData> activeComposters = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    private final File dataFile;
    private final NamespacedKey holoKey;

    public ComposterManager(GardenCore plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "composters.yml");
        this.holoKey = new NamespacedKey(plugin, "gc_composter_hologram");

        loadComposters();
        startTicker();
    }

    // ── Persistence ───────────────────────────────────────────

    private void loadComposters() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        if (!config.contains("composters")) return;

        for (String key : config.getConfigurationSection("composters").getKeys(false)) {
            String path = "composters." + key + ".";
            String worldName = config.getString(path + "world");
            if (worldName == null || plugin.getServer().getWorld(worldName) == null) continue;

            int x = config.getInt(path + "x");
            int y = config.getInt(path + "y");
            int z = config.getInt(path + "z");
            Location loc = new Location(plugin.getServer().getWorld(worldName), x, y, z);

            String placer = config.getString(path + "placer");
            String typeStr = config.getString(path + "type");
            ComposterData.ComposterType type;
            try {
                type = ComposterData.ComposterType.valueOf(typeStr);
            } catch (Exception e) {
                continue; // Skip if invalid type
            }

            long placedAt = config.getLong(path + "placedAt");
            int duration = config.getInt(path + "duration");
            double radius = config.getDouble(path + "radius");
            List<String> holoLines = config.getStringList(path + "holoLines");

            ComposterData data = new ComposterData(loc, placer, type, placedAt, duration, radius, holoLines);

            if (data.getRemainingSeconds() > 0) {
                // Clean up any leftover holograms from before the restart/crash
                cleanupOldHolograms(loc);

                spawnHologram(data);
                activeComposters.put(key(loc), data);
            } else {
                // Expired while the server was offline
                plugin.getServer().getScheduler().runTask(plugin, () -> loc.getBlock().setType(org.bukkit.Material.AIR));
            }
        }
    }

    private void saveComposters() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, ComposterData> entry : activeComposters.entrySet()) {
            // Replace colons and dots to create a safe YAML path key
            String safeKey = entry.getKey().replace(":", "_").replace(".", "_");
            ComposterData data = entry.getValue();
            String path = "composters." + safeKey + ".";

            config.set(path + "world", data.getBlockLocation().getWorld().getName());
            config.set(path + "x", data.getBlockLocation().getBlockX());
            config.set(path + "y", data.getBlockLocation().getBlockY());
            config.set(path + "z", data.getBlockLocation().getBlockZ());
            config.set(path + "placer", data.getPlacerName());
            config.set(path + "type", data.getType().name());
            config.set(path + "placedAt", data.getPlacedAtMillis());
            config.set(path + "duration", data.getDurationSeconds());
            config.set(path + "radius", data.getRadius());
            config.set(path + "holoLines", data.getHologramLines());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save composters.yml: " + e.getMessage());
        }
    }

    private void cleanupOldHolograms(Location loc) {
        int cx = loc.getBlockX() >> 4;
        int cz = loc.getBlockZ() >> 4;

        // Load the chunk if it isn't loaded so we can access the entities inside it
        if (!loc.getWorld().isChunkLoaded(cx, cz)) {
            loc.getWorld().loadChunk(cx, cz);
        }

        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 5, 2)) {
            if (entity instanceof ArmorStand && entity.getPersistentDataContainer().has(holoKey, PersistentDataType.BYTE)) {
                entity.remove();
            }
        }
    }

    // ── Placement ─────────────────────────────────────────────

    public void placeComposter(Player player, Location blockLoc,
                               ComposterData.ComposterType type,
                               int durationSeconds, double radius,
                               List<String> hologramLines) {
        ComposterData data = new ComposterData(
                blockLoc,
                player.getName(),
                type,
                System.currentTimeMillis(),
                durationSeconds,
                radius,
                hologramLines
        );

        spawnHologram(data);
        activeComposters.put(key(blockLoc), data);
        saveComposters(); // Save immediately in case of a crash
    }

    public void placeComposter(Player player, Location blockLoc,
                               ComposterData.ComposterType type,
                               int durationSeconds, double radius) {
        placeComposter(player, blockLoc, type, durationSeconds, radius, List.of());
    }

    // ── Removal ───────────────────────────────────────────────

    public void removeComposter(Location blockLoc) {
        ComposterData data = activeComposters.remove(key(blockLoc));
        if (data != null) {
            despawnHologram(data);
            saveComposters(); // Save immediately
        }
    }

    public boolean isComposter(Location loc) {
        return activeComposters.containsKey(key(loc));
    }

    // ── Buff query ────────────────────────────────────────────

    public double getTotalFiberBonus(Location playerLoc) {
        return sumBonus(playerLoc, ComposterData.ComposterType.FIBER_100)
                + sumBonus(playerLoc, ComposterData.ComposterType.FIBER_250);
    }

    public double getTotalXpBonus(Location playerLoc) {
        return sumBonus(playerLoc, ComposterData.ComposterType.XP_50)
                + sumBonus(playerLoc, ComposterData.ComposterType.XP_75);
    }

    public double getTotalMaterialAmountBonus(Location playerLoc) {
        return sumBonus(playerLoc, ComposterData.ComposterType.MATERIAL_100)
                + sumBonus(playerLoc, ComposterData.ComposterType.MATERIAL_150);
    }

    private double sumBonus(Location playerLoc, ComposterData.ComposterType type) {
        double total = 0;
        for (ComposterData data : activeComposters.values()) {
            if (data.getType() != type) continue;
            if (!isSameWorld(playerLoc, data.getBlockLocation())) continue;
            if (playerLoc.distance(data.getBlockLocation()) <= data.getRadius()) {
                total += type.getBonusPercent();
            }
        }
        return total;
    }

    // ── Hologram ──────────────────────────────────────────────

    private void spawnHologram(ComposterData data) {
        Location base = data.getBlockLocation().clone().add(0.5, 1.0, 0.5);
        String[] lines = buildLines(data);
        List<ArmorStand> stands = new ArrayList<>();
        // Lines are ordered bottom → top; spawn from index 0 upward
        for (int i = 0; i < lines.length; i++) {
            stands.add(spawnTextStand(base.clone().add(0, i * 0.28, 0), lines[i]));
        }
        data.setHologramStands(stands);
    }

    private void despawnHologram(ComposterData data) {
        for (ArmorStand stand : data.getHologramStands()) {
            if (stand != null && stand.isValid()) stand.remove();
        }
        data.getHologramStands().clear();
    }

    private void updateHologramLines(ComposterData data) {
        String[] lines = buildLines(data);
        List<ArmorStand> stands = data.getHologramStands();

        // If the number of lines changed (shouldn't normally happen but be safe), respawn
        if (stands.size() != lines.length) {
            despawnHologram(data);
            spawnHologram(data);
            return;
        }

        for (int i = 0; i < Math.min(lines.length, stands.size()); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && stand.isValid()) {
                stand.setCustomName(ColorUtil.translate(lines[i]));
            }
        }
    }

    private String[] buildLines(ComposterData data) {
        String timeRemaining = formatTime(data.getRemainingSeconds());
        String placer        = data.getPlacerName();
        String typeName      = data.getType().getDisplayColor() + "&l" + data.getType().getDisplayName();
        String radius        = String.valueOf((int) data.getRadius());
        String duration      = String.valueOf(data.getDurationSeconds());

        if (data.hasCustomHologramLines()) {
            List<String> configured = data.getHologramLines();
            String[] result = new String[configured.size()];
            for (int i = 0; i < configured.size(); i++) {
                result[i] = resolvePlaceholders(configured.get(i),
                        timeRemaining, placer, typeName, radius, duration);
            }
            return result;
        }

        // Default built-in lines (bottom → top)
        return new String[]{
                "&7Expires in: &f" + timeRemaining,
                "&7Placed by: &e" + placer,
                typeName,
                "&#FFD700&l✦ LUCKY COMPOSTER ✦"
        };
    }

    private String resolvePlaceholders(String line, String time, String placer,
                                       String type, String radius, String duration) {
        return line
                .replace("{time}",     time)
                .replace("{placer}",   placer)
                .replace("{type}",     type)
                .replace("{radius}",   radius)
                .replace("{duration}", duration);
    }

    private ArmorStand spawnTextStand(Location loc, String text) {
        return loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(ColorUtil.translate(text));
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setSmall(true);
            stand.setPersistent(false); // Make sure it isn't saved natively into the chunk
            stand.getPersistentDataContainer().set(
                    holoKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    // ── Ticker ────────────────────────────────────────────────

    private void startTicker() {
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            List<String> expired = new ArrayList<>();

            for (Map.Entry<String, ComposterData> entry : activeComposters.entrySet()) {
                ComposterData data = entry.getValue();
                if (data.getRemainingSeconds() <= 0) {
                    expired.add(entry.getKey());
                    Location loc = data.getBlockLocation();
                    if (loc.getWorld() != null) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> loc.getBlock().setType(org.bukkit.Material.AIR));
                    }
                } else {
                    updateHologramLines(data);
                }
            }

            if (!expired.isEmpty()) {
                for (String k : expired) {
                    ComposterData data = activeComposters.remove(k);
                    if (data != null) despawnHologram(data);
                }
                saveComposters(); // Only hit disk if an expiration occurred
            }
        }, 20L, 20L);
    }

    // ── Helpers ───────────────────────────────────────────────

    private String key(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private boolean isSameWorld(Location a, Location b) {
        return a.getWorld() != null && b.getWorld() != null
                && a.getWorld().getName().equals(b.getWorld().getName());
    }

    private String formatTime(long seconds) {
        long m = seconds / 60, s = seconds % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        saveComposters();
        for (ComposterData data : activeComposters.values()) despawnHologram(data);
        activeComposters.clear();
    }

    public Collection<ComposterData> getActiveComposters() {
        return Collections.unmodifiableCollection(activeComposters.values());
    }
}