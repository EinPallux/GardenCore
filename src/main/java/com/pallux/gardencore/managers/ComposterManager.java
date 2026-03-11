package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.ComposterData;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ComposterManager {

    private final GardenCore plugin;

    private final Map<String, ComposterData> activeComposters = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public ComposterManager(GardenCore plugin) {
        this.plugin = plugin;
        startTicker();
    }

    // ── Placement ─────────────────────────────────────────────

    /**
     * Called when a player places a composter item.
     * hologramLines comes from the item's hologram-lines list in items.yml.
     * If empty, buildLines() falls back to the default built-in lines.
     */
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
    }

    /**
     * Legacy overload for callers that don't supply hologram lines.
     * Falls back to built-in default lines.
     */
    public void placeComposter(Player player, Location blockLoc,
                               ComposterData.ComposterType type,
                               int durationSeconds, double radius) {
        placeComposter(player, blockLoc, type, durationSeconds, radius, List.of());
    }

    // ── Removal ───────────────────────────────────────────────

    public void removeComposter(Location blockLoc) {
        ComposterData data = activeComposters.remove(key(blockLoc));
        if (data != null) despawnHologram(data);
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

    /**
     * Builds the hologram lines array for a composter.
     *
     * If the item had hologram-lines configured in items.yml, those are used.
     * The following placeholders are resolved in each line:
     *   {time}        — formatted time remaining (e.g. "14m 23s")
     *   {placer}      — name of the player who placed it
     *   {type}        — display name of the composter type (e.g. "+100% Fiber")
     *   {radius}      — buff radius in blocks
     *   {duration}    — total duration in seconds
     *
     * If no custom lines are configured, the default built-in lines are used.
     * Lines are ordered bottom → top (index 0 = bottom stand).
     */
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
            stand.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "gc_composter_hologram"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
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

            for (String k : expired) {
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
        long m = seconds / 60, s = seconds % 60;
        return m > 0 ? m + "m " + s + "s" : s + "s";
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        for (ComposterData data : activeComposters.values()) despawnHologram(data);
        activeComposters.clear();
    }

    public Collection<ComposterData> getActiveComposters() {
        return Collections.unmodifiableCollection(activeComposters.values());
    }
}