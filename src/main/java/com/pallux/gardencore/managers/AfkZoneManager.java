package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AfkZoneManager {

    private final GardenCore plugin;

    private boolean enabled;
    private int rewardInterval;
    private String rewardCommand;
    private String titleText;
    private String subtitleText;
    private String actionBarEnter;
    private String actionBarLeave;

    private String worldName;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private boolean zoneConfigured;

    private final Set<UUID> playersInZone = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> playerTimers = new ConcurrentHashMap<>();

    private BukkitTask tickTask;

    public AfkZoneManager(GardenCore plugin) {
        this.plugin = plugin;
        load();
        startTicker();
    }

    public void load() {
        FileConfiguration cfg = plugin.getConfigManager().getAfkZone();

        enabled = cfg.getBoolean("afkzone.enabled", true);
        rewardInterval = cfg.getInt("afkzone.reward-interval", 60);
        rewardCommand = cfg.getString("afkzone.reward-command", "");
        titleText = cfg.getString("afkzone.title", "&#a8ff78&lAFK Zone");
        subtitleText = cfg.getString("afkzone.subtitle", "&#7a7a7a Next Reward in: &f{time}s");
        actionBarEnter = cfg.getString("afkzone.actionbar-enter", "&#a8ff78✦ Entered the AFK Zone");
        actionBarLeave = cfg.getString("afkzone.actionbar-leave", "&#ff7a7a✦ Left the AFK Zone");

        worldName = cfg.getString("afkzone.zone.world", "");
        minX = cfg.getDouble("afkzone.zone.min-x", 0);
        minY = cfg.getDouble("afkzone.zone.min-y", 0);
        minZ = cfg.getDouble("afkzone.zone.min-z", 0);
        maxX = cfg.getDouble("afkzone.zone.max-x", 0);
        maxY = cfg.getDouble("afkzone.zone.max-y", 0);
        maxZ = cfg.getDouble("afkzone.zone.max-z", 0);

        zoneConfigured = !worldName.isEmpty() && !(minX == 0 && maxX == 0 && minZ == 0 && maxZ == 0);
    }

    private void startTicker() {
        if (tickTask != null) tickTask.cancel();

        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!enabled || !zoneConfigured) return;

            for (UUID uuid : playersInZone) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    playersInZone.remove(uuid);
                    playerTimers.remove(uuid);
                    continue;
                }

                int timeLeft = playerTimers.merge(uuid, -1, Integer::sum);

                if (timeLeft <= 0) {
                    playerTimers.put(uuid, rewardInterval);
                    String cmd = rewardCommand.replace("%player%", player.getName());
                    plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), cmd);
                    timeLeft = rewardInterval;
                }

                sendTitle(player, timeLeft);
            }
        }, 20L, 20L);
    }

    public void checkEnter(Player player) {
        if (!enabled || !zoneConfigured) return;
        UUID uuid = player.getUniqueId();

        if (isInZone(player.getLocation()) && !playersInZone.contains(uuid)) {
            playersInZone.add(uuid);
            playerTimers.put(uuid, rewardInterval);
            sendActionBar(player, ColorUtil.translate(actionBarEnter));
        }
    }

    public void checkLeave(Player player, Location from, Location to) {
        if (!enabled || !zoneConfigured) return;
        UUID uuid = player.getUniqueId();

        boolean wasInside = playersInZone.contains(uuid);
        boolean nowInside = isInZone(to);

        if (wasInside && !nowInside) {
            playersInZone.remove(uuid);
            playerTimers.remove(uuid);
            sendActionBar(player, ColorUtil.translate(actionBarLeave));
            clearTitle(player);
        } else if (!wasInside && nowInside) {
            playersInZone.add(uuid);
            playerTimers.put(uuid, rewardInterval);
            sendActionBar(player, ColorUtil.translate(actionBarEnter));
        }
    }

    public void handleQuit(Player player) {
        playersInZone.remove(player.getUniqueId());
        playerTimers.remove(player.getUniqueId());
    }

    private boolean isInZone(Location loc) {
        if (loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        double x = loc.getX(), y = loc.getY(), z = loc.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private void sendTitle(Player player, int timeLeft) {
        String title = ColorUtil.translate(titleText);
        String subtitle = ColorUtil.translate(subtitleText.replace("{time}", String.valueOf(timeLeft)));
        player.sendTitle(title, subtitle, 0, 25, 5);
    }

    private void clearTitle(Player player) {
        player.sendTitle("", "", 0, 1, 5);
    }

    private void sendActionBar(Player player, String message) {
        player.sendActionBar(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().deserialize(message));
    }

    public void setZone(String worldName, double x1, double y1, double z1, double x2, double y2, double z2) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
        this.zoneConfigured = true;
        saveZone();
    }

    private void saveZone() {
        File file = new File(plugin.getDataFolder(), "settings/afkzone.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        cfg.set("afkzone.zone.world", worldName);
        cfg.set("afkzone.zone.min-x", minX);
        cfg.set("afkzone.zone.min-y", minY);
        cfg.set("afkzone.zone.min-z", minZ);
        cfg.set("afkzone.zone.max-x", maxX);
        cfg.set("afkzone.zone.max-y", maxY);
        cfg.set("afkzone.zone.max-z", maxZ);

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save settings/afkzone.yml: " + e.getMessage());
        }
    }

    public boolean isZoneConfigured() { return zoneConfigured; }
    public boolean isEnabled() { return enabled; }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
    }
}