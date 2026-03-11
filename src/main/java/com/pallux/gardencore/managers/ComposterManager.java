package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.ComposterData;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ComposterManager {

    private final GardenCore plugin;

    /** block location key → active composter data */
    private final Map<String, ComposterData> activeComposters = new ConcurrentHashMap<>();

    private BukkitTask tickTask;

    public ComposterManager(GardenCore plugin) {
        this.plugin = plugin;
        startTicker();
    }

    // ── Placement ─────────────────────────────────────────────

    /**
     * Called when a player places a composter item.
     * Spawns the hologram stands and schedules the despawn.
     */
    public void placeComposter(Player player, Location blockLoc, ComposterData.ComposterType type) {
        int durationSeconds = plugin.getConfigManager().getConfig()
                .getInt("composters.duration-seconds", 900);

        ComposterData data = new ComposterData(
                blockLoc,
                player.getName(),
                type,
                System.currentTimeMillis(),
                durationSeconds
        );

        spawnHologram(data);
        activeComposters.put(key(blockLoc), data);
    }

    // ── Removal ───────────────────────────────────────────────

    /** Removes a composter at the given location (block broken / timer expired). */
    public void removeComposter(Location blockLoc) {
        ComposterData data = activeComposters.remove(key(blockLoc));
        if (data != null) despawnHologram(data);
    }

    /** Returns true if there is an active composter at this location. */
    public boolean isComposter(Location loc) {
        return activeComposters.containsKey(key(loc));
    }

    // ── Buff query ────────────────────────────────────────────

    /**
     * Total fiber bonus (percentage) for a player from all nearby composters.
     * Applied inside MultiplierManager just like event bonuses.
     */
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
        double radius = plugin.getConfigManager().getConfig()
                .getDouble("composters.radius", 20.0);
        double total = 0;
        for (ComposterData data : activeComposters.values()) {
            if (data.getType() != type) continue;
            if (!isSameWorld(playerLoc, data.getBlockLocation())) continue;
            if (playerLoc.distance(data.getBlockLocation()) <= radius) {
                total += type.getBonusPercent();
            }
        }
        return total;
    }

    // ── Hologram ──────────────────────────────────────────────

    private void spawnHologram(ComposterData data) {
        Location base = data.getBlockLocation().clone().add(0.5, 1.0, 0.5);

        // Lines from bottom to top (highest ArmorStand = top line)
        String[] lines = buildLines(data);
        List<ArmorStand> stands = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            Location lineLoc = base.clone().add(0, i * 0.28, 0);
            ArmorStand stand = spawnTextStand(lineLoc, lines[i]);
            stands.add(stand);
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
        for (int i = 0; i < Math.min(lines.length, stands.size()); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && stand.isValid()) {
                stand.setCustomName(ColorUtil.translate(lines[i]));
            }
        }
    }

    /**
     * Lines array — index 0 is the BOTTOM line, higher indices are above it.
     * Order (bottom → top):
     *   [0] time remaining
     *   [1] placer name
     *   [2] type label
     *   [3] "LUCKY COMPOSTER" header
     */
    private String[] buildLines(ComposterData data) {
        long remaining = data.getRemainingSeconds();
        String timeStr = formatTime(remaining);
        return new String[]{
                "&7Expires in: &f" + timeStr,
                "&7Placed by: &e" + data.getPlacerName(),
                data.getType().getDisplayColor() + "&l" + data.getType().getDisplayName(),
                "&#FFD700&l✦ LUCKY COMPOSTER ✦"
        };
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
            stand.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "gc_composter_hologram"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1
            );
        });
    }

    // ── Ticker ────────────────────────────────────────────────

    private void startTicker() {
        // Every 20 ticks (1 second): update hologram time, expire finished composters
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            List<String> toRemove = new ArrayList<>();

            for (Map.Entry<String, ComposterData> entry : activeComposters.entrySet()) {
                ComposterData data = entry.getValue();

                if (data.getRemainingSeconds() <= 0) {
                    toRemove.add(entry.getKey());
                    // Remove the block from the world
                    Location loc = data.getBlockLocation();
                    if (loc.getWorld() != null) {
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> loc.getBlock().setType(org.bukkit.Material.AIR));
                    }
                } else {
                    // Update time display every second
                    updateHologramLines(data);
                }
            }

            for (String k : toRemove) {
                ComposterData data = activeComposters.remove(k);
                if (data != null) despawnHologram(data);
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
        long m = seconds / 60;
        long s = seconds % 60;
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (ComposterData data : activeComposters.values()) {
            despawnHologram(data);
        }
        activeComposters.clear();
    }

    public Collection<ComposterData> getActiveComposters() {
        return Collections.unmodifiableCollection(activeComposters.values());
    }
}