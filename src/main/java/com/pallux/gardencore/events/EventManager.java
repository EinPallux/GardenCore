package com.pallux.gardencore.events;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.EventData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class EventManager {

    private static final String SCHEDULED_EVENT_ID = "__scheduled__";

    private final GardenCore plugin;
    private final List<EventData> availableEvents = new ArrayList<>();
    private final Random random = new Random();

    private final Map<String, ActiveEvent> activeEvents = new LinkedHashMap<>();
    private BukkitTask eventScheduler;
    private BossBar idleBossBar;

    public EventManager(GardenCore plugin) {
        this.plugin = plugin;
        loadEvents();
        setupIdleBossBar();
        startScheduler();
    }

    private static class ActiveEvent {
        final EventData data;
        final BossBar bossBar;
        BukkitTask timer;
        int timeLeft;

        ActiveEvent(EventData data, BossBar bossBar, int timeLeft) {
            this.data = data;
            this.bossBar = bossBar;
            this.timeLeft = timeLeft;
        }
    }

    private void loadEvents() {
        availableEvents.clear();
        ConfigurationSection section = plugin.getConfigManager().getEventsConfig().getConfigurationSection("events");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String displayName = section.getString(key + ".display-name", key);
            String typeName = section.getString(key + ".type", "FIBER_AMOUNT");
            double value = section.getDouble(key + ".value", 0);

            try {
                EventData.EventType type = EventData.EventType.valueOf(typeName);
                availableEvents.add(new EventData(key, displayName, type, value));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown event type '" + typeName + "' for event '" + key + "'");
            }
        }

        plugin.getLogger().info("Loaded " + availableEvents.size() + " events from events.yml");
    }

    private void setupIdleBossBar() {
        String noneText = ColorUtil.translate(plugin.getConfigManager().getMessage("events.bossbar-none"));
        idleBossBar = Bukkit.createBossBar(noneText, BarColor.GREEN, BarStyle.SOLID);
        idleBossBar.setVisible(true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            idleBossBar.addPlayer(player);
        }
    }

    private void startScheduler() {
        if (!plugin.getConfigManager().isFeatureEnabled("events")) return;

        int intervalTicks = plugin.getConfigManager().getEventInterval() * 60 * 20;

        eventScheduler = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () ->
                        plugin.getServer().getScheduler().runTask(plugin, this::startRandomScheduledEvent),
                intervalTicks, intervalTicks);
    }

    private void startRandomScheduledEvent() {
        if (availableEvents.isEmpty()) return;
        EventData chosen = availableEvents.get(random.nextInt(availableEvents.size()));
        launchEvent(SCHEDULED_EVENT_ID, chosen, true);
    }

    private void launchEvent(String id, EventData data, boolean wasIdle) {
        if (activeEvents.containsKey(id)) {
            endEventById(id, false);
        }

        int durationSeconds = plugin.getConfigManager().getEventDuration() * 60;

        BossBar bar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(player);
        }

        ActiveEvent active = new ActiveEvent(data, bar, durationSeconds);
        updateActiveBossBar(active);

        active.timer = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            active.timeLeft--;
            updateActiveBossBar(active);
            if (active.timeLeft <= 0) {
                endEventById(id, true);
            }
        }, 20L, 20L);

        activeEvents.put(id, active);

        // Hide idle bar now that at least one event is active
        if (wasIdle) {
            idleBossBar.setVisible(false);
        }

        String startMsg = plugin.getConfigManager().getMessage("events.start")
                .replace("{event}", data.getDisplayName());
        MessageUtil.broadcast(startMsg);
    }

    private void endEventById(String id, boolean broadcast) {
        ActiveEvent active = activeEvents.remove(id);
        if (active == null) return;

        if (active.timer != null) active.timer.cancel();
        active.bossBar.removeAll();

        if (broadcast) {
            String endMsg = plugin.getConfigManager().getMessage("events.end")
                    .replace("{event}", active.data.getDisplayName());
            MessageUtil.broadcast(endMsg);
        }

        if (activeEvents.isEmpty()) {
            idleBossBar.setVisible(true);
        }
    }

    private void updateActiveBossBar(ActiveEvent active) {
        String text = plugin.getConfigManager().getMessage("events.bossbar-active")
                .replace("{event}", active.data.getDisplayName())
                .replace("{time}", String.valueOf(active.timeLeft));
        active.bossBar.setTitle(ColorUtil.translate(text));
    }

    public void addPlayer(Player player) {
        if (activeEvents.isEmpty()) {
            idleBossBar.addPlayer(player);
        } else {
            // Player joins during active events — show all active event bars, not the idle one
            for (ActiveEvent active : activeEvents.values()) {
                active.bossBar.addPlayer(player);
            }
        }
    }

    public void removePlayer(Player player) {
        idleBossBar.removePlayer(player);
        for (ActiveEvent active : activeEvents.values()) {
            active.bossBar.removePlayer(player);
        }
    }

    public boolean isEventActive() {
        return !activeEvents.isEmpty();
    }

    public EventData getActiveEvent() {
        if (activeEvents.isEmpty()) return null;
        return activeEvents.values().iterator().next().data;
    }

    public List<EventData> getAllActiveEvents() {
        return activeEvents.values().stream().map(a -> a.data).toList();
    }

    public double getTotalEventBonus(EventData.EventType type) {
        return activeEvents.values().stream()
                .filter(a -> a.data.getType() == type)
                .mapToDouble(a -> a.data.getValue())
                .sum();
    }

    public List<String> getAvailableEventKeys() {
        return availableEvents.stream().map(EventData::getKey).toList();
    }

    public boolean startEventByKey(String key) {
        return startEventByKey(key, false);
    }

    public boolean startEventByKey(String key, boolean fromTicket) {
        EventData event = availableEvents.stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .findFirst()
                .orElse(null);
        if (event == null) return false;

        String id = fromTicket ? "ticket_" + key + "_" + System.currentTimeMillis() : SCHEDULED_EVENT_ID;
        boolean wasIdle = activeEvents.isEmpty();
        launchEvent(id, event, wasIdle);
        return true;
    }

    public boolean stopCurrentEvent() {
        if (!activeEvents.containsKey(SCHEDULED_EVENT_ID)) return false;
        endEventById(SCHEDULED_EVENT_ID, true);
        return true;
    }

    public void shutdown() {
        if (eventScheduler != null) eventScheduler.cancel();
        new ArrayList<>(activeEvents.keySet()).forEach(id -> endEventById(id, false));
        if (idleBossBar != null) idleBossBar.removeAll();
    }
}