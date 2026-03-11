package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.BossData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BossManager {

    private final GardenCore plugin;

    /** All bosses loaded from bosses.yml, keyed by config key */
    private final Map<String, BossData> bosses = new LinkedHashMap<>();

    /** Active boss bars — one per active boss */
    private final Map<String, BossBar> bossBars = new HashMap<>();

    /** Per-boss countdown task */
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();

    /**
     * Per-boss roam task — moves the body stand (and name stand) to a new
     * random position inside the zone every few seconds.
     */
    private final Map<String, BukkitTask> roamTasks = new HashMap<>();

    /** Main scheduler that fires the spawn cycle */
    private BukkitTask spawnScheduler;

    private final Random random = new Random();

    // ── Roam configuration ────────────────────────────────────
    /** How often (in ticks) the boss picks a new roam target. 20 ticks = 1 s. */
    private static final long ROAM_INTERVAL_TICKS = 60L; // every 3 seconds
    /** How many blocks per tick the stand moves toward its target. */
    private static final double ROAM_SPEED = 0.15;

    public BossManager(GardenCore plugin) {
        this.plugin = plugin;
        loadBosses();
        startSpawnScheduler();
    }

    // ── Loading ────────────────────────────────────────────────

    public void loadBosses() {
        bosses.clear();
        FileConfiguration cfg = plugin.getConfigManager().getBossesConfig();
        ConfigurationSection sec = cfg.getConfigurationSection("bosses");
        if (sec == null) return;

        for (String key : sec.getKeys(false)) {
            String path = "bosses." + key + ".";

            String displayName   = cfg.getString(path + "display-name", key);
            String islandKey     = cfg.getString(path + "island-key", key);
            String skullTexture  = cfg.getString(path + "skull-texture", "");
            double size          = cfg.getDouble(path + "size", 3.0);
            double maxHealth     = cfg.getDouble(path + "max-health", 10000.0);
            int durationSeconds  = cfg.getInt(path + "duration-seconds", 300);
            String rewardCommand = cfg.getString(path + "reward-command", "");

            BossData data = new BossData(key, displayName, maxHealth, durationSeconds,
                    islandKey, skullTexture, size, rewardCommand);

            // Load saved zone
            String world = cfg.getString(path + "zone.world", "");
            double minX  = cfg.getDouble(path + "zone.min-x", 0);
            double minY  = cfg.getDouble(path + "zone.min-y", 0);
            double minZ  = cfg.getDouble(path + "zone.min-z", 0);
            double maxX  = cfg.getDouble(path + "zone.max-x", 0);
            double maxY  = cfg.getDouble(path + "zone.max-y", 0);
            double maxZ  = cfg.getDouble(path + "zone.max-z", 0);

            if (!world.isEmpty() && !(minX == 0 && maxX == 0 && minZ == 0 && maxZ == 0)) {
                data.setZone(world, minX, minY, minZ, maxX, maxY, maxZ);
            }

            bosses.put(key, data);
        }

        plugin.getLogger().info("Loaded " + bosses.size() + " bosses from bosses.yml");
    }

    // ── Spawn scheduler ────────────────────────────────────────

    private void startSpawnScheduler() {
        if (spawnScheduler != null) spawnScheduler.cancel();

        FileConfiguration cfg = plugin.getConfigManager().getBossesConfig();
        int intervalMinutes = cfg.getInt("spawn-interval-minutes", 30);
        long intervalTicks  = (long) intervalMinutes * 60 * 20;

        spawnScheduler = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::spawnAllReadyBosses, intervalTicks, intervalTicks);
    }

    /** Spawns every boss whose zone is set and that is not already active. */
    private void spawnAllReadyBosses() {
        for (BossData boss : bosses.values()) {
            if (!boss.isZoneSet() || boss.isActive()) continue;
            spawnBoss(boss);
        }
    }

    // ── Spawning ───────────────────────────────────────────────

    public boolean spawnBoss(BossData boss) {
        if (boss.isActive()) return false;
        if (!boss.isZoneSet()) return false;

        Location loc = boss.getSpawnLocation();
        if (loc == null || loc.getWorld() == null) return false;

        boss.reset();

        // ── Body stand (the giant skull) ───────────────────────
        ArmorStand body = loc.getWorld().spawn(loc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(false);
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setSmall(false);

            // Lock all equipment slots so nothing can be placed on it
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                stand.addEquipmentLock(slot, ArmorStand.LockType.ADDING_OR_CHANGING);
            }

            // Apply scale attribute for the giant size
            try {
                AttributeInstance scaleAttr = stand.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.setBaseValue(Math.max(0.01, Math.min(16.0, boss.getSize())));
                }
            } catch (Exception e) {
                // Attribute may not exist on older builds — silently ignore
            }

            stand.getEquipment().setHelmet(buildSkullItem(boss));

            stand.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "gc_boss_key"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    boss.getKey()
            );
        });

        // ── Name hologram stand — shows display-name only ──────
        double nameOffsetY = boss.getSize() * 1.6 + 0.5;
        Location nameLoc = loc.clone().add(0, nameOffsetY, 0);

        ArmorStand nameStand = loc.getWorld().spawn(nameLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(ColorUtil.translate(boss.getDisplayName()));
            stand.setInvulnerable(true);
            stand.setSilent(true);
            stand.setSmall(true);

            stand.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "gc_boss_name_stand"),
                    org.bukkit.persistence.PersistentDataType.BYTE,
                    (byte) 1
            );
        });

        boss.setBodyStand(body);
        boss.setNameStand(nameStand);
        boss.setActive(true);
        boss.setSpawnedAt(System.currentTimeMillis());

        // ── Boss bar ───────────────────────────────────────────
        BossBar bar = Bukkit.createBossBar(
                buildBossBarTitle(boss),
                BarColor.RED,
                BarStyle.SEGMENTED_10
        );
        bar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);
        bossBars.put(boss.getKey(), bar);

        // ── Chat announcement ──────────────────────────────────
        String spawnMsg = plugin.getConfigManager().getMessage("boss.spawned")
                .replace("{boss}",     ColorUtil.translate(boss.getDisplayName()))
                .replace("{island}",   boss.getIslandKey())
                .replace("{duration}", String.valueOf(boss.getDurationSeconds() / 60));
        plugin.getServer().broadcastMessage(ColorUtil.translate(spawnMsg));

        // ── Countdown / expiry task ────────────────────────────
        startCountdown(boss);

        // ── Roaming movement task ──────────────────────────────
        startRoaming(boss);

        return true;
    }

    // ── Countdown ─────────────────────────────────────────────

    private void startCountdown(BossData boss) {
        BukkitTask old = countdownTasks.remove(boss.getKey());
        if (old != null) old.cancel();

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!boss.isActive()) {
                cancelCountdown(boss.getKey());
                return;
            }

            long elapsed   = (System.currentTimeMillis() - boss.getSpawnedAt()) / 1000L;
            long remaining = boss.getDurationSeconds() - elapsed;

            if (remaining <= 0) {
                expireBoss(boss);
                cancelCountdown(boss.getKey());
                return;
            }

            // Update boss bar progress and title
            BossBar bar = bossBars.get(boss.getKey());
            if (bar != null) {
                double healthFraction = boss.getCurrentHealth() / boss.getMaxHealth();
                bar.setProgress(Math.max(0.0, Math.min(1.0, healthFraction)));
                bar.setTitle(buildBossBarTitle(boss));
            }

            // Hologram stays as display-name only — no update needed

        }, 20L, 20L);

        countdownTasks.put(boss.getKey(), task);
    }

    private void cancelCountdown(String key) {
        BukkitTask t = countdownTasks.remove(key);
        if (t != null) t.cancel();
    }

    // ── Roaming ────────────────────────────────────────────────

    /**
     * Starts a repeating task that:
     *   1. Every ROAM_INTERVAL_TICKS picks a new random target inside the boss zone.
     *   2. Every tick smoothly slides the body stand (and name stand) toward the target.
     *
     * Both stands move together so the hologram stays above the skull.
     */
    private void startRoaming(BossData boss) {
        cancelRoaming(boss.getKey());

        // Mutable state held in arrays so the lambda can capture them
        final Location[] target   = { null };
        final long[]     ticksSinceNewTarget = { 0L };
        final double     nameOffsetY = boss.getSize() * 1.6 + 0.5;

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!boss.isActive()) {
                cancelRoaming(boss.getKey());
                return;
            }

            ArmorStand body = boss.getBodyStand();
            ArmorStand ns   = boss.getNameStand();

            if (body == null || !body.isValid()) return;

            // Pick a new roam target periodically
            ticksSinceNewTarget[0]++;
            if (target[0] == null || ticksSinceNewTarget[0] >= ROAM_INTERVAL_TICKS) {
                target[0] = randomLocationInZone(boss, body.getWorld());
                ticksSinceNewTarget[0] = 0;
            }

            if (target[0] == null) return;

            Location current = body.getLocation();
            double dx = target[0].getX() - current.getX();
            double dz = target[0].getZ() - current.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);

            // Already close enough — wait for the next target
            if (dist < 0.2) return;

            double step = Math.min(ROAM_SPEED, dist);
            double nx = current.getX() + (dx / dist) * step;
            double nz = current.getZ() + (dz / dist) * step;
            // Y stays at zone floor (minY) — we don't need vertical movement
            double ny = target[0].getY();

            Location newLoc = new Location(current.getWorld(), nx, ny, nz,
                    current.getYaw(), current.getPitch());

            body.teleport(newLoc);

            // Keep name stand locked above body
            if (ns != null && ns.isValid()) {
                ns.teleport(new Location(current.getWorld(), nx, ny + nameOffsetY, nz));
            }

        }, 1L, 1L);

        roamTasks.put(boss.getKey(), task);
    }

    private void cancelRoaming(String key) {
        BukkitTask t = roamTasks.remove(key);
        if (t != null) t.cancel();
    }

    /**
     * Returns a random Location within the boss zone, snapped to the floor (minY).
     * A small inset keeps the boss away from the very edge of the zone.
     */
    private Location randomLocationInZone(BossData boss, World world) {
        double inset = Math.max(1.0, boss.getSize());
        double minX  = boss.getMinX() + inset;
        double maxX  = boss.getMaxX() - inset;
        double minZ  = boss.getMinZ() + inset;
        double maxZ  = boss.getMaxZ() - inset;

        // Zone too small for inset — fall back to exact bounds
        if (minX > maxX) { minX = boss.getMinX(); maxX = boss.getMaxX(); }
        if (minZ > maxZ) { minZ = boss.getMinZ(); maxZ = boss.getMaxZ(); }

        double x = minX + random.nextDouble() * (maxX - minX);
        double z = minZ + random.nextDouble() * (maxZ - minZ);
        double y = boss.getMinY();

        return new Location(world, x, y, z);
    }

    // ── Damage ─────────────────────────────────────────────────

    /**
     * Called by BossListener when a player attacks the body stand.
     * Damage = the player's total fiber multiplier.
     */
    public void handleHit(Player player, BossData boss) {
        if (!boss.isActive()) return;

        double damage = plugin.getMultiplierManager().getTotalFiberMultiplier(player.getUniqueId());
        boss.setCurrentHealth(boss.getCurrentHealth() - damage);
        boss.getDamageMap().merge(player.getUniqueId(), damage, Double::sum);

        // Immediate boss bar update
        BossBar bar = bossBars.get(boss.getKey());
        if (bar != null) {
            double fraction = boss.getCurrentHealth() / boss.getMaxHealth();
            bar.setProgress(Math.max(0.0, Math.min(1.0, fraction)));
            bar.setTitle(buildBossBarTitle(boss));
        }

        if (boss.getCurrentHealth() <= 0) {
            defeatBoss(boss);
        }
    }

    // ── Defeat ─────────────────────────────────────────────────

    private void defeatBoss(BossData boss) {
        cancelCountdown(boss.getKey());
        cancelRoaming(boss.getKey());

        Map<UUID, Double> snapshot = new LinkedHashMap<>(boss.getDamageMap());
        despawnBoss(boss);

        String defeatMsg = plugin.getConfigManager().getMessage("boss.defeated")
                .replace("{boss}",   ColorUtil.translate(boss.getDisplayName()))
                .replace("{island}", boss.getIslandKey());
        plugin.getServer().broadcastMessage(ColorUtil.translate(defeatMsg));

        for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;

            String cmd = boss.getRewardCommand()
                    .replace("%player%", p.getName())
                    .replace("%damage%",  NumberUtil.formatRaw(entry.getValue()));

            if (!cmd.isBlank()) {
                plugin.getServer().dispatchCommand(
                        plugin.getServer().getConsoleSender(), cmd);
            }
        }
    }

    private void expireBoss(BossData boss) {
        cancelRoaming(boss.getKey());
        despawnBoss(boss);

        String expireMsg = plugin.getConfigManager().getMessage("boss.expired")
                .replace("{boss}",   ColorUtil.translate(boss.getDisplayName()))
                .replace("{island}", boss.getIslandKey());
        plugin.getServer().broadcastMessage(ColorUtil.translate(expireMsg));
    }

    // ── Despawn ────────────────────────────────────────────────

    private void despawnBoss(BossData boss) {
        ArmorStand body = boss.getBodyStand();
        if (body != null && body.isValid()) body.remove();

        ArmorStand nameStand = boss.getNameStand();
        if (nameStand != null && nameStand.isValid()) nameStand.remove();

        BossBar bar = bossBars.remove(boss.getKey());
        if (bar != null) bar.removeAll();

        boss.reset();
    }

    // ── Boss bar title ─────────────────────────────────────────
    // Shows: BossName | HP / MaxHP | Time remaining
    // Format is driven by the "boss.bossbar" message key in messages.yml
    // Placeholders: {boss}, {hp}, {max_hp}, {time}

    private String buildBossBarTitle(BossData boss) {
        String hpStr    = NumberUtil.formatRaw(boss.getCurrentHealth());
        String maxHpStr = NumberUtil.formatRaw(boss.getMaxHealth());
        long elapsed    = boss.isActive()
                ? (System.currentTimeMillis() - boss.getSpawnedAt()) / 1000L : 0;
        long remaining  = Math.max(0, boss.getDurationSeconds() - elapsed);

        return ColorUtil.translate(
                plugin.getConfigManager().getMessage("boss.bossbar")
                        .replace("{boss}",   boss.getDisplayName())
                        .replace("{hp}",     hpStr)
                        .replace("{max_hp}", maxHpStr)
                        .replace("{time}",   String.valueOf(remaining))
        );
    }

    // ── Skull item builder ─────────────────────────────────────

    private org.bukkit.inventory.ItemStack buildSkullItem(BossData boss) {
        org.bukkit.inventory.ItemStack item =
                new org.bukkit.inventory.ItemStack(Material.PLAYER_HEAD);

        if (boss.getSkullTexture() == null || boss.getSkullTexture().isBlank()) {
            return item;
        }

        org.bukkit.inventory.meta.SkullMeta meta =
                (org.bukkit.inventory.meta.SkullMeta) item.getItemMeta();
        if (meta == null) return item;

        try {
            String decoded = new String(Base64.getDecoder().decode(boss.getSkullTexture().trim()));
            int start = decoded.indexOf("\"url\":\"") + 7;
            int end   = decoded.indexOf("\"", start);
            if (start >= 7 && end > start) {
                String url = decoded.substring(start, end);
                org.bukkit.profile.PlayerProfile profile =
                        Bukkit.createPlayerProfile(UUID.randomUUID(), "BossCosmetic");
                org.bukkit.profile.PlayerTextures textures = profile.getTextures();
                textures.setSkin(new java.net.URL(url));
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("BossManager: failed to apply skull texture for boss '"
                    + boss.getKey() + "': " + e.getMessage());
        }

        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }

    // ── Player join/leave ──────────────────────────────────────

    public void addPlayer(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.addPlayer(player);
        }
    }

    public void removePlayer(Player player) {
        for (BossBar bar : bossBars.values()) {
            bar.removePlayer(player);
        }
    }

    // ── Zone persistence ───────────────────────────────────────

    public void saveZone(BossData boss) {
        File file = new File(plugin.getDataFolder(), "settings/bosses.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        String path = "bosses." + boss.getKey() + ".zone.";
        cfg.set(path + "world", boss.getWorldName());
        cfg.set(path + "min-x", boss.getMinX());
        cfg.set(path + "min-y", boss.getMinY());
        cfg.set(path + "min-z", boss.getMinZ());
        cfg.set(path + "max-x", boss.getMaxX());
        cfg.set(path + "max-y", boss.getMaxY());
        cfg.set(path + "max-z", boss.getMaxZ());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save settings/bosses.yml: " + e.getMessage());
        }
    }

    // ── Public API ─────────────────────────────────────────────

    public BossData getBoss(String key) {
        return bosses.get(key);
    }

    public BossData getBossByStandUUID(UUID standUUID) {
        for (BossData boss : bosses.values()) {
            ArmorStand body = boss.getBodyStand();
            if (body != null && body.getUniqueId().equals(standUUID)) return boss;
        }
        return null;
    }

    public Collection<BossData> getAllBosses() {
        return Collections.unmodifiableCollection(bosses.values());
    }

    public Set<String> getBossKeys() {
        return Collections.unmodifiableSet(bosses.keySet());
    }

    public boolean forceSpawn(String key) {
        BossData boss = bosses.get(key);
        if (boss == null) return false;
        return spawnBoss(boss);
    }

    public boolean forceDespawn(String key) {
        BossData boss = bosses.get(key);
        if (boss == null || !boss.isActive()) return false;
        cancelCountdown(key);
        cancelRoaming(key);
        despawnBoss(boss);
        return true;
    }

    // ── Shutdown ───────────────────────────────────────────────

    public void shutdown() {
        if (spawnScheduler != null) spawnScheduler.cancel();

        for (String key : new ArrayList<>(countdownTasks.keySet())) cancelCountdown(key);
        for (String key : new ArrayList<>(roamTasks.keySet()))      cancelRoaming(key);

        for (BossData boss : bosses.values()) {
            if (boss.isActive()) despawnBoss(boss);
        }
    }
}