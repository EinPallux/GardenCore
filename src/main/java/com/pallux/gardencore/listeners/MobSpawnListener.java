package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
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

public class MobSpawnListener implements Listener {

    /**
     * Squared reach distance for arm-swing hit detection.
     * Generous enough to feel responsive without being exploitable.
     */
    private static final double HIT_REACH_SQ = 4.5 * 4.5;

    private final GardenCore plugin;
    private final NamespacedKey ownerKey;

    public MobSpawnListener(GardenCore plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "gc_crop_muncher_owner");
    }

    // ── LEFT-CLICK: arm swing ─────────────────────────────────
    //
    // ArmorStands are spawned with setInvulnerable(true), so
    // EntityDamageByEntityEvent is never fired for them on Paper 1.21.
    // PlayerAnimationEvent fires on every left-click swing regardless of
    // invulnerability, making it the only reliable hook — same pattern
    // used by BossListener for the same reason.
    //
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) return;

        Player   player = event.getPlayer();
        Location eyeLoc = player.getEyeLocation();

        for (Entity nearby : player.getNearbyEntities(5, 5, 5)) {
            if (!(nearby instanceof ArmorStand stand)) continue;
            if (!isCropMuncher(stand)) continue;

            // Distance check against the centre of the stand
            Location standCenter = stand.getLocation().clone().add(0, 1.0, 0);
            if (eyeLoc.distanceSquared(standCenter) > HIT_REACH_SQ) continue;

            plugin.getMobSpawnManager().handleHit(player, stand);
            break; // one hit per swing
        }
    }

    // ── RIGHT-CLICK: block interaction ────────────────────────
    //
    // Prevents equipping items, posing the stand, etc.
    //
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;
        if (!isCropMuncher(stand)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof ArmorStand stand)) return;
        if (!isCropMuncher(stand)) return;
        event.setCancelled(true);
    }

    // ── Helper ────────────────────────────────────────────────

    private boolean isCropMuncher(ArmorStand stand) {
        return stand.getPersistentDataContainer().has(ownerKey, PersistentDataType.STRING);
    }
}