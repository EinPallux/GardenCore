package com.pallux.gardencore.listeners;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.CropData;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.MessageUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CropFarmingListener implements Listener {

    private final GardenCore plugin;
    private final Map<UUID, Long> lastBreakTime = new ConcurrentHashMap<>();

    public CropFarmingListener(GardenCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Optional<CropData> optCrop = plugin.getCropManager().getCropData(event.getBlock().getType());
        if (optCrop.isEmpty()) return;

        CropData crop = optCrop.get();
        int playerLevel = plugin.getLevelManager().getLevel(player.getUniqueId());

        if (playerLevel < crop.getLevelRequired()) {
            event.setCancelled(true);
            MessageUtil.send(player, "crop.level-required", Map.of(
                    "level", String.valueOf(crop.getLevelRequired()),
                    "crop", crop.getDisplayName()
            ));
            return;
        }

        UUID uuid = player.getUniqueId();
        double cooldownSeconds = plugin.getUpgradeManager().getEffectiveCropCooldown(player);
        long cooldownMillis = (long) (cooldownSeconds * 1000);
        long now = System.currentTimeMillis();
        long last = lastBreakTime.getOrDefault(uuid, 0L);

        if (now - last < cooldownMillis) {
            return;
        }

        lastBreakTime.put(uuid, now);

        double totalMultiplier = plugin.getMultiplierManager().getTotalFiberMultiplier(uuid);
        double earned = crop.getFiber() * totalMultiplier;

        plugin.getFiberManager().addFiberFromCrop(player, crop.getFiber());
        plugin.getLevelManager().addXp(player, crop.getXp());

        if (plugin.getConfigManager().isFeatureEnabled("material-drops")) {
            plugin.getMaterialManager().rollDrops(player);
        }

        // Roll for pet on every crop break
        plugin.getPetManager().rollForPet(player);

        sendFiberTitle(player, earned);

        plugin.getDataManager().saveAsync();
    }

    private void sendFiberTitle(Player player, double earned) {
        String amountStr = NumberUtil.formatRaw(earned);

        String titleTemplate    = plugin.getConfigManager().getMessage("fiber.harvest-title");
        String subtitleTemplate = plugin.getConfigManager().getMessage("fiber.harvest-subtitle");

        String title    = ColorUtil.translate(titleTemplate.replace("{amount}", amountStr));
        String subtitle = ColorUtil.translate(subtitleTemplate.replace("{amount}", amountStr));

        player.sendTitle(title, subtitle, 2, 18, 6);
    }
}