package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobSpawnManager {

    private final GardenCore plugin;
    private final Random random = new Random();

    // ── Per-muncher state ─────────────────────────────────────

    /** Body stand UUID → owning player UUID */
    private final Map<UUID, UUID> cropMuncherOwners = new ConcurrentHashMap<>();

    /** Body stand UUID → despawn task */
    private final Map<UUID, BukkitTask> despawnTasks = new ConcurrentHashMap<>();

    /** Body stand UUID → ticker task */
    private final Map<UUID, BukkitTask> tickerTasks = new ConcurrentHashMap<>();

    /** Body stand UUID → current hit count */
    private final Map<UUID, Integer> hitCounts = new ConcurrentHashMap<>();

    /** Body stand UUID → spawn timestamp ms */
    private final Map<UUID, Long> spawnTimes = new ConcurrentHashMap<>();

    /**
     * Body stand UUID → hologram stands, index 0 = line1 (top), 1 = line2, 2 = line3 (bottom).
     */
    private final Map<UUID, List<ArmorStand>> holoStands = new ConcurrentHashMap<>();

    public MobSpawnManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Config helpers ────────────────────────────────────────

    private boolean isEnabled() {
        return cfg().getBoolean("mob-spawns.enabled", true)
                && cfg().getBoolean("mob-spawns.crop-muncher.enabled", true);
    }

    private double getSpawnChance()     { return cfg().getDouble("mob-spawns.crop-muncher.spawn-chance", 0.01); }
    private int    getHealth()          { return cfg().getInt("mob-spawns.crop-muncher.health", 20); }
    private int    getDespawnSeconds()  { return cfg().getInt("mob-spawns.crop-muncher.despawn-seconds", 15); }
    private double getFiberMultiplier() { return cfg().getDouble("mob-spawns.crop-muncher.fiber-multiplier", 10.0); }
    private String getDisplayName()     { return cfg().getString("mob-spawns.crop-muncher.display-name", "&#ff4444&l☠ Crop Muncher"); }
    private double getLineSpacing()     { return cfg().getDouble("mob-spawns.crop-muncher.hologram.line-spacing", 0.28); }
    private String getLineTemplate(int n) { return cfg().getString("mob-spawns.crop-muncher.hologram.line-" + n, ""); }

    private Color getArmorColor(String key, Color fallback) {
        String hex = cfg().getString("mob-spawns.crop-muncher.armor." + key, "");
        if (hex == null || hex.isBlank()) return fallback;
        try {
            int rgb = Integer.parseInt(hex.replace("#", ""), 16);
            return Color.fromRGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private org.bukkit.configuration.file.FileConfiguration cfg() {
        return plugin.getConfigManager().getMobSpawnsConfig();
    }

    // ── Helpers ───────────────────────────────────────────────

    /** Green → yellow → red based on fraction of HP remaining. */
    private String hpColor(int hpRemaining, int maxHp) {
        double frac = (double) hpRemaining / maxHp;
        if (frac > 0.6) return "&#a8ff78";
        if (frac > 0.3) return "&#FFD700";
        return "&#ff4444";
    }

    /** Resolves all placeholders in a line template and translates color codes. */
    private String resolveLine(int lineNum, int hpRemaining, int maxHp, int secondsRemaining, String playerName) {
        String template = getLineTemplate(lineNum);
        if (template.isBlank()) return " ";
        String hpCol = hpColor(hpRemaining, maxHp);
        return ColorUtil.translate(template
                .replace("{name}",     ColorUtil.translate(getDisplayName()))
                .replace("{hp_color}", hpCol)
                .replace("{hp_dot}",   hpCol + "█")
                .replace("{hp}",       String.valueOf(hpRemaining))
                .replace("{max_hp}",   String.valueOf(maxHp))
                .replace("{time}",     String.valueOf(secondsRemaining))
                .replace("{player}",   playerName));
    }

    // ── Spawn roll ────────────────────────────────────────────

    public void rollForCropMuncher(org.bukkit.entity.Player player) {
        if (!isEnabled()) return;
        if (random.nextDouble() * 100.0 >= getSpawnChance()) return;
        spawnCropMuncher(player);
    }

    // ── Spawning ──────────────────────────────────────────────

    private void spawnCropMuncher(org.bukkit.entity.Player player) {
        // Snapshot config values once so every part of this spawn uses identical values
        final int    maxHp   = getHealth();
        final int    maxSecs = getDespawnSeconds();
        final long   spawnAt = System.currentTimeMillis();
        final String pName   = player.getName();

        Location bodyLoc = player.getLocation().clone().add(
                player.getLocation().getDirection().normalize().multiply(2)
        );
        bodyLoc.setY(bodyLoc.getBlockY() + 1);

        NamespacedKey ownerKey = new NamespacedKey(plugin, "gc_crop_muncher_owner");
        NamespacedKey holoKey  = new NamespacedKey(plugin, "gc_crop_muncher_holo");

        // ── Body stand ─────────────────────────────────────
        ArmorStand body = bodyLoc.getWorld().spawn(bodyLoc, ArmorStand.class, s -> {
            s.setVisible(true);
            s.setGravity(true);
            s.setCanPickupItems(false);
            s.setCustomNameVisible(false);
            s.setInvulnerable(true);
            s.setSilent(false);
            s.setSmall(false);
            s.setArms(true);

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                s.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
            }

            s.getEquipment().setHelmet(buildZombieHead());
            s.getEquipment().setChestplate(buildLeatherPiece(Material.LEATHER_CHESTPLATE,
                    getArmorColor("chestplate-color", Color.fromRGB(0x2E, 0x6B, 0x1F))));
            s.getEquipment().setLeggings(buildLeatherPiece(Material.LEATHER_LEGGINGS,
                    getArmorColor("leggings-color",   Color.fromRGB(0x1A, 0x47, 0x10))));
            s.getEquipment().setBoots(buildLeatherPiece(Material.LEATHER_BOOTS,
                    getArmorColor("boots-color",      Color.fromRGB(0x1A, 0x47, 0x10))));

            s.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING,
                    player.getUniqueId().toString());
        });

        UUID bodyId = body.getUniqueId();

        // Register state BEFORE spawning holos or starting tasks so every
        // subsequent read finds a consistent initial value of 0 hits / maxHp hp.
        cropMuncherOwners.put(bodyId, player.getUniqueId());
        hitCounts.put(bodyId, 0);
        spawnTimes.put(bodyId, spawnAt);

        // ── Hologram stands ────────────────────────────────
        // index 0 = line1 (top), index 1 = line2 (middle), index 2 = line3 (bottom)
        // Each line sits above the previous one by line-spacing blocks.
        // Base Y is just above the body stand's head.
        double spacing   = getLineSpacing();
        double baseHoloY = bodyLoc.getY() + 2.1;

        List<ArmorStand> holos = new ArrayList<>(3);
        for (int idx = 0; idx < 3; idx++) {
            int    lineNum     = idx + 1; // idx 0 → line1, idx 1 → line2, idx 2 → line3
            double holoY       = baseHoloY + (2 - idx) * spacing; // line1 highest, line3 lowest
            Location hLoc      = bodyLoc.clone();
            hLoc.setY(holoY);

            // Use the already-registered maxHp and 0 hits for the initial text
            String initialText = resolveLine(lineNum, maxHp, maxHp, maxSecs, pName);

            ArmorStand holo = bodyLoc.getWorld().spawn(hLoc, ArmorStand.class, s -> {
                s.setVisible(false);
                s.setGravity(false);
                s.setCanPickupItems(false);
                s.setCustomNameVisible(true);
                s.setCustomName(initialText);
                s.setInvulnerable(true);
                s.setSilent(true);
                s.setSmall(true);
                s.getPersistentDataContainer().set(holoKey, PersistentDataType.BYTE, (byte) 1);
            });
            holos.add(holo);
        }
        holoStands.put(bodyId, holos);

        // Notify the owning player
        com.pallux.gardencore.utils.MessageUtil.send(player, "mob-spawns.crop-muncher-spawned");

        // ── Ticker ─────────────────────────────────────────
        BukkitTask ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!body.isValid()) { cancelTicker(bodyId); return; }
            int hitsLeft = maxHp - hitCounts.getOrDefault(bodyId, 0);
            int secsLeft = (int) Math.max(0, maxSecs - (System.currentTimeMillis() - spawnAt) / 1000L);
            refreshHologram(bodyId, body, hitsLeft, maxHp, secsLeft, pName);
        }, 20L, 20L);
        tickerTasks.put(bodyId, ticker);

        // ── Auto-despawn ───────────────────────────────────
        BukkitTask despawn = plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> removeCropMuncher(bodyId, false), (long) maxSecs * 20L);
        despawnTasks.put(bodyId, despawn);
    }

    // ── Hologram refresh ──────────────────────────────────────

    /**
     * Updates the text of each holo stand and re-anchors them above the body
     * (which may have shifted slightly due to gravity).
     *
     * holos index 0 = line1 (top), 1 = line2, 2 = line3 (bottom).
     */
    private void refreshHologram(UUID bodyId, ArmorStand body,
                                 int hpRemaining, int maxHp,
                                 int secsRemaining, String playerName) {
        List<ArmorStand> holos = holoStands.get(bodyId);
        if (holos == null) return;

        double spacing   = getLineSpacing();
        double baseHoloY = body.getLocation().getY() + 2.1;

        for (int idx = 0; idx < holos.size(); idx++) {
            ArmorStand holo = holos.get(idx);
            if (holo == null || !holo.isValid()) continue;

            int    lineNum = idx + 1;
            double holoY   = baseHoloY + (2 - idx) * spacing;

            holo.setCustomName(resolveLine(lineNum, hpRemaining, maxHp, secsRemaining, playerName));

            Location newLoc = body.getLocation().clone();
            newLoc.setY(holoY);
            holo.teleport(newLoc);
        }
    }

    // ── Hit handling ──────────────────────────────────────────

    public boolean handleHit(org.bukkit.entity.Player player, ArmorStand stand) {
        UUID bodyId    = stand.getUniqueId();
        UUID ownerUUID = cropMuncherOwners.get(bodyId);

        if (ownerUUID == null) return false;
        if (!ownerUUID.equals(player.getUniqueId())) return false;

        int maxHp    = getHealth();
        int newCount = hitCounts.merge(bodyId, 1, Integer::sum);
        int hpLeft   = maxHp - newCount;

        // Instant refresh so the bar reacts to every hit, not just every second
        long spawnedAt = spawnTimes.getOrDefault(bodyId, System.currentTimeMillis());
        int  secsLeft  = (int) Math.max(0,
                getDespawnSeconds() - (System.currentTimeMillis() - spawnedAt) / 1000L);
        refreshHologram(bodyId, stand, hpLeft, maxHp, secsLeft, player.getName());

        if (newCount >= maxHp) {
            killCropMuncher(player, stand);
        }
        return true;
    }

    private void killCropMuncher(org.bukkit.entity.Player player, ArmorStand body) {
        UUID bodyId = body.getUniqueId();

        cancelTicker(bodyId);
        BukkitTask dt = despawnTasks.remove(bodyId);
        if (dt != null) dt.cancel();

        despawnHologram(bodyId);
        cropMuncherOwners.remove(bodyId);
        hitCounts.remove(bodyId);
        spawnTimes.remove(bodyId);

        double reward = plugin.getMultiplierManager().getTotalFiberMultiplier(player.getUniqueId())
                * getFiberMultiplier();
        plugin.getFiberManager().giveFiber(player.getUniqueId(), reward);
        plugin.getDataManager().savePlayerAsync(player.getUniqueId());

        String amountStr = NumberUtil.formatRaw(reward);
        String title    = ColorUtil.translate(
                plugin.getConfigManager().getMessage("fiber.harvest-title").replace("{amount}", amountStr));
        String subtitle = ColorUtil.translate(
                plugin.getConfigManager().getMessage("fiber.harvest-subtitle").replace("{amount}", amountStr));
        player.sendTitle(title, subtitle, 2, 30, 10);

        body.getWorld().spawnParticle(Particle.SMOKE,
                body.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
        body.remove();
    }

    // ── Removal ───────────────────────────────────────────────

    private void removeCropMuncher(UUID bodyId, boolean alreadyRemoved) {
        cancelTicker(bodyId);
        BukkitTask dt = despawnTasks.remove(bodyId);
        if (dt != null) dt.cancel();

        despawnHologram(bodyId);
        cropMuncherOwners.remove(bodyId);
        hitCounts.remove(bodyId);
        spawnTimes.remove(bodyId);

        if (!alreadyRemoved) {
            for (World world : Bukkit.getWorlds()) {
                Entity entity = world.getEntity(bodyId);
                if (entity != null) { entity.remove(); break; }
            }
        }
    }

    private void despawnHologram(UUID bodyId) {
        List<ArmorStand> holos = holoStands.remove(bodyId);
        if (holos == null) return;
        for (ArmorStand holo : holos) {
            if (holo != null && holo.isValid()) holo.remove();
        }
    }

    private void cancelTicker(UUID bodyId) {
        BukkitTask t = tickerTasks.remove(bodyId);
        if (t != null) t.cancel();
    }

    // ── Public query ──────────────────────────────────────────

    public boolean isCropMuncher(ArmorStand stand) {
        return cropMuncherOwners.containsKey(stand.getUniqueId());
    }

    public UUID getOwner(ArmorStand stand) {
        return cropMuncherOwners.get(stand.getUniqueId());
    }

    // ── Item builders ─────────────────────────────────────────

    private ItemStack buildZombieHead() {
        ItemStack item = new ItemStack(Material.ZOMBIE_HEAD);
        var meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack buildLeatherPiece(Material material, Color color) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    // ── Shutdown ──────────────────────────────────────────────

    public void shutdown() {
        tickerTasks.values().forEach(BukkitTask::cancel);
        despawnTasks.values().forEach(BukkitTask::cancel);
        for (UUID bodyId : new HashSet<>(cropMuncherOwners.keySet())) {
            despawnHologram(bodyId);
            for (World world : Bukkit.getWorlds()) {
                Entity entity = world.getEntity(bodyId);
                if (entity != null) { entity.remove(); break; }
            }
        }
        tickerTasks.clear();
        despawnTasks.clear();
        cropMuncherOwners.clear();
        hitCounts.clear();
        spawnTimes.clear();
        holoStands.clear();
    }
}