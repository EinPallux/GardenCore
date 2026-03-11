package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.BossData;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

public class BossListener implements Listener {

    /**
     * Squared reach distance for arm-swing hit detection.
     * Vanilla survival melee reach is 3 blocks; 4.5 gives a comfortable margin
     * for the giant boss stands without being exploitably large.
     */
    private static final double HIT_REACH_SQ = 4.5 * 4.5;

    private final GardenCore plugin;
    private final NamespacedKey bossKey;
    private final NamespacedKey bossNameKey;

    public BossListener(GardenCore plugin) {
        this.plugin      = plugin;
        this.bossKey     = new NamespacedKey(plugin, "gc_boss_key");
        this.bossNameKey = new NamespacedKey(plugin, "gc_boss_name_stand");
    }

    // ── LEFT-CLICK: PlayerAnimationEvent (arm swing) ──────────────────────────
    //
    // Why not EntityDamageByEntityEvent?
    //
    // The boss body stand is spawned with setInvulnerable(true).
    // On Paper 1.21, invulnerable entities are protected at the lowest level of
    // the damage pipeline — EntityDamageByEntityEvent is never fired for them at
    // all, regardless of EventPriority or ignoreCancelled setting.
    //
    // PlayerAnimationEvent fires on EVERY left-click swing and is not gated by
    // invulnerability, so it is the only reliable left-click hook available.
    //
    // We iterate the player's nearby entities and check whether any boss body
    // stand falls within melee reach. If so, we register the hit.
    // Only one hit is awarded per swing even if multiple boss stands are nearby.
    //
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player   player    = event.getPlayer();
        Location eyeLoc    = player.getEyeLocation();

        for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
            if (!(nearby instanceof ArmorStand stand)) continue;

            String key = stand.getPersistentDataContainer()
                    .get(bossKey, PersistentDataType.STRING);
            if (key == null) continue; // name stands and non-boss stands have no bossKey

            BossData boss = plugin.getBossManager().getBoss(key);
            if (boss == null || !boss.isActive()) continue;

            // Check distance — aim at the vertical centre of the stand
            Location standCenter = stand.getLocation().clone().add(0, boss.getSize() * 0.5, 0);
            if (eyeLoc.distanceSquared(standCenter) > HIT_REACH_SQ) continue;

            plugin.getBossManager().handleHit(player, boss);
            break; // one hit per swing
        }
    }

    // ── RIGHT-CLICK: PlayerInteractAtEntityEvent ──────────────────────────────
    //
    // Fires when a player right-clicks an entity.
    // We cancel the event to prevent item-in-hand side effects (placing blocks,
    // equipping items on the stand, etc.) and route to handleHit.
    //
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Entity entity = event.getRightClicked();
        if (!(entity instanceof ArmorStand stand)) return;

        boolean isNameStand = stand.getPersistentDataContainer()
                .has(bossNameKey, PersistentDataType.BYTE);
        String key = stand.getPersistentDataContainer()
                .get(bossKey, PersistentDataType.STRING);

        if (key == null && !isNameStand) return; // not one of ours

        event.setCancelled(true);

        if (isNameStand) return; // block interaction but no hit

        BossData boss = plugin.getBossManager().getBoss(key);
        if (boss == null || !boss.isActive()) return;

        plugin.getBossManager().handleHit(event.getPlayer(), boss);
    }

    // ── BLOCK all other right-click interactions on boss stands ───────────────
    //
    // Prevents equipping items, posing the stand, etc. via the non-precise
    // PlayerInteractEntityEvent (fires in addition to PlayerInteractAtEntityEvent).
    //
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;

        if (stand.getPersistentDataContainer().has(bossKey,     PersistentDataType.STRING)
                || stand.getPersistentDataContainer().has(bossNameKey, PersistentDataType.BYTE)) {
            event.setCancelled(true);
        }
    }
}