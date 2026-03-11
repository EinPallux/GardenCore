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
        this.plugin      = plugin;
        this.bossKey     = new NamespacedKey(plugin, "gc_boss_key");
        this.bossNameKey = new NamespacedKey(plugin, "gc_boss_name_stand");
    }

    /**
     * PRIMARY HIT HANDLER — EntityDamageByEntityEvent.
     *
     * On Paper 1.21, when a player left-clicks an ArmorStand the server fires
     * EntityDamageByEntityEvent with damage = 0 (because ArmorStands are not
     * living entities). This is the most reliable trigger for "player attacked
     * an ArmorStand" across Paper versions.
     *
     * We cancel the vanilla damage and route through our own hit logic.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) return;
        if (!(event.getDamager() instanceof Player player)) return;

        // Always cancel vanilla damage on boss stands to prevent unintended side-effects
        String key = stand.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);
        boolean isNameStand = stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE);

        if (key == null && !isNameStand) return; // Not one of ours — leave it alone

        event.setCancelled(true);
        event.setDamage(0);

        if (isNameStand) return; // Name stand — cancel interaction but do no damage

        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null || !boss.isActive()) return;

        plugin.getBossManager().handleHit(player, boss);
    }

    /**
     * SECONDARY HIT HANDLER — PlayerInteractAtEntityEvent (right-click on entity).
     *
     * Some Paper builds fire this instead of (or in addition to)
     * EntityDamageByEntityEvent for certain click styles. We handle it here as
     * a fallback so both left- and right-click register as hits on the boss.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand stand)) return;

        // Cancel so items in hand don't trigger extra actions (e.g. placing blocks)
        boolean isNameStand = stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE);
        String key = stand.getPersistentDataContainer().get(bossKey, PersistentDataType.STRING);

        if (key == null && !isNameStand) return; // Not ours — leave it alone

        event.setCancelled(true);

        if (isNameStand) return;

        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null || !boss.isActive()) return;

        Player player = event.getPlayer();
        plugin.getBossManager().handleHit(player, boss);
    }

    /**
     * Prevent any other right-click entity interactions on boss stands
     * (e.g. equipping items, posing).
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