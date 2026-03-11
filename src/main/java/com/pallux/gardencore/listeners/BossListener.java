package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.BossData;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class BossListener implements Listener {

    private final GardenCore plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey bossNameKey;

    public BossListener(GardenCore plugin) {
        this.plugin     = plugin;
        this.bossKey     = new NamespacedKey(plugin, "gc_boss_key");
        this.bossNameKey = new NamespacedKey(plugin, "gc_boss_name_stand");
    }

    /**
     * ArmorStands do not fire EntityDamageByEntityEvent when attacked by players
     * in vanilla because they are not living entities that take normal damage.
     * Instead, a left-click on an ArmorStand fires PlayerInteractAtEntityEvent.
     * We use that as our "hit" trigger.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // Only main hand clicks count
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand stand)) return;

        // Cancel interaction so items in hand don't trigger (e.g. placing blocks)
        event.setCancelled(true);

        // Block interactions with the name/health stand
        if (stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE)) return;

        // Check if this is a boss body stand
        String key = stand.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (key == null) return;

        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null || !boss.isActive()) return;

        Player player = event.getPlayer();
        plugin.getBossManager().handleHit(player, boss);
    }

    /**
     * PlayerInteractAtEntityEvent covers right-click-on-entity.
     * For left-click (attack) on ArmorStands, Paper fires EntityDamageByEntityEvent
     * with a damage of 0. We catch that too so either click style works.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        // Cancel vanilla damage — we handle it ourselves
        event.setCancelled(true);
        event.setDamage(0);

        // Block interactions with the name/health stand
        if (stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE)) return;

        String key = stand.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        if (key == null) return;

        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null || !boss.isActive()) return;

        plugin.getBossManager().handleHit(player, boss);
    }

    /**
     * Also cancel right-click entity events on boss stands to prevent
     * any unintended interactions (e.g. opening poses, equipping items).
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        if (stand.getPersistentDataContainer().has(bossKey, PersistentDataType.STRING)
                || stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}