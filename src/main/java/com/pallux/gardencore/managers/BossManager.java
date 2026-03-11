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

    /** Main scheduler that fires the spawn cycle */
    private BukkitTask spawnScheduler;

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
            String hologramFormat = cfg.getString(path + "hologram-format",
                    "{boss}\n&#FF6B6B❤ &7{hp} &8/ {max_hp}");

            BossData data = new BossData(key, displayName, maxHealth, durationSeconds,
                    islandKey, skullTexture, size, rewardCommand, hologramFormat);

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
        if (!boss.isZoneSet())  return false;

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

            // Lock all equipment slots
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

            // Build and apply the skull helmet
            stand.getEquipment().setHelmet(buildSkullItem(boss));

            // PDC tag so we can identify it on interact
            stand.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "gc_boss_key"),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    boss.getKey()
            );
        });

        // ── Name / health hologram stand ───────────────────────
        // Positioned slightly above the body stand
        double nameOffsetY = boss.getSize() * 1.6 + 0.5;
        Location nameLoc = loc.clone().add(0, nameOffsetY, 0);

        ArmorStand nameStand = loc.getWorld().spawn(nameLoc, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCanPickupItems(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(buildHealthDisplay(boss));
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

        return true;
    }

    private void startCountdown(BossData boss) {
        // Cancel any old task for this key
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
                // Time ran out — despawn with no reward
                expireBoss(boss);
                cancelCountdown(boss.getKey());
                return;
            }

            // Update boss bar progress
            BossBar bar = bossBars.get(boss.getKey());
            if (bar != null) {
                double healthFraction = boss.getCurrentHealth() / boss.getMaxHealth();
                bar.setProgress(Math.max(0.0, Math.min(1.0, healthFraction)));
                bar.setTitle(buildBossBarTitle(boss));
            }

            // Update hologram
            ArmorStand ns = boss.getNameStand();
            if (ns != null && ns.isValid()) {
                ns.setCustomName(buildHealthDisplay(boss));
            }

        }, 20L, 20L);

        countdownTasks.put(boss.getKey(), task);
    }

    private void cancelCountdown(String key) {
        BukkitTask t = countdownTasks.remove(key);
        if (t != null) t.cancel();
    }

    // ── Damage ─────────────────────────────────────────────────

    /**
     * Called by BossListener when a player left-clicks the body stand.
     * Damage = player's total fiber multiplier.
     */
    public void handleHit(Player player, BossData boss) {
        if (!boss.isActive()) return;

        double damage = plugin.getMultiplierManager().getTotalFiberMultiplier(player.getUniqueId());
        boss.setCurrentHealth(boss.getCurrentHealth() - damage);

        // Track damage per player
        boss.getDamageMap().merge(player.getUniqueId(), damage, Double::sum);

        // Update hologram immediately
        ArmorStand ns = boss.getNameStand();
        if (ns != null && ns.isValid()) {
            ns.setCustomName(buildHealthDisplay(boss));
        }

        // Update boss bar immediately
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

        // Snapshot damage map before reset
        Map<UUID, Double> snapshot = new LinkedHashMap<>(boss.getDamageMap());

        despawnBoss(boss);

        // Broadcast defeat
        String defeatMsg = plugin.getConfigManager().getMessage("boss.defeated")
                .replace("{boss}",   ColorUtil.translate(boss.getDisplayName()))
                .replace("{island}", boss.getIslandKey());
        plugin.getServer().broadcastMessage(ColorUtil.translate(defeatMsg));

        // Issue rewards to every player who dealt damage
        for (Map.Entry<UUID, Double> entry : snapshot.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue; // offline players get nothing

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
        despawnBoss(boss);

        String expireMsg = plugin.getConfigManager().getMessage("boss.expired")
                .replace("{boss}",   ColorUtil.translate(boss.getDisplayName()))
                .replace("{island}", boss.getIslandKey());
        plugin.getServer().broadcastMessage(ColorUtil.translate(expireMsg));
    }

    // ── Despawn ────────────────────────────────────────────────

    private void despawnBoss(BossData boss) {
        // Remove ArmorStands
        ArmorStand body = boss.getBodyStand();
        if (body != null && body.isValid()) body.remove();

        ArmorStand nameStand = boss.getNameStand();
        if (nameStand != null && nameStand.isValid()) nameStand.remove();

        // Remove boss bar
        BossBar bar = bossBars.remove(boss.getKey());
        if (bar != null) {
            bar.removeAll();
        }

        boss.reset();
    }

    // ── Boss bar / hologram text builders ──────────────────────

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

    /**
     * Builds the hologram text from the per-boss hologram-format field in bosses.yml.
     * Placeholders: {boss}, {hp}, {max_hp}
     * The \n literal in the YAML string is treated as a real newline so multiple
     * lines can be packed into a single ArmorStand name (legacy display).
     */
    private String buildHealthDisplay(BossData boss) {
        String hpStr    = NumberUtil.formatRaw(boss.getCurrentHealth());
        String maxHpStr = NumberUtil.formatRaw(boss.getMaxHealth());

        String formatted = boss.getHologramFormat()
                .replace("{boss}",   boss.getDisplayName())
                .replace("{hp}",     hpStr)
                .replace("{max_hp}", maxHpStr)
                .replace("\\n", "\n");

        return ColorUtil.translate(formatted);
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

    /** Add a joining player to all active boss bars. */
    public void addPlayer(Player player) {
        for (Map.Entry<String, BossBar> entry : bossBars.entrySet()) {
            entry.getValue().addPlayer(player);
        }
    }

    /** Remove a leaving player from all boss bars. */
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

    /** Returns a BossData by config key, or null if not found. */
    public BossData getBoss(String key) {
        return bosses.get(key);
    }

    /** Finds a BossData whose body ArmorStand matches the given entity UUID. */
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

    /** Force-spawn a specific boss by key (admin command). */
    public boolean forceSpawn(String key) {
        BossData boss = bosses.get(key);
        if (boss == null) return false;
        return spawnBoss(boss);
    }

    /** Force-despawn a specific boss by key (admin command). */
    public boolean forceDespawn(String key) {
        BossData boss = bosses.get(key);
        if (boss == null || !boss.isActive()) return false;
        cancelCountdown(key);
        despawnBoss(boss);
        return true;
    }

    // ── Shutdown ───────────────────────────────────────────────

    public void shutdown() {
        if (spawnScheduler != null) spawnScheduler.cancel();

        for (String key : new ArrayList<>(countdownTasks.keySet())) {
            cancelCountdown(key);
        }

        for (BossData boss : bosses.values()) {
            if (boss.isActive()) despawnBoss(boss);
        }
    }
}