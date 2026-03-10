package com.pallux.gardencore.managers;

import com.pallux.gardencore.GardenCore;
import com.pallux.gardencore.models.PetRarity;
import com.pallux.gardencore.utils.ColorUtil;
import com.pallux.gardencore.utils.NumberUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.UUID;

public class PetManager {

    private final GardenCore plugin;
    private final Random random = new Random();

    public PetManager(GardenCore plugin) {
        this.plugin = plugin;
    }

    // ── Config helpers ─────────────────────────────────────────

    private FileConfiguration cfg() {
        return plugin.getConfigManager().getPetsConfig();
    }

    private ConfigurationSection raritySection(PetRarity rarity) {
        return cfg().getConfigurationSection("pets.rarities." + rarity.getConfigKey());
    }

    /** 1-in-N drop chance for this rarity (0 = disabled). */
    public int getOneInChance(PetRarity rarity) {
        ConfigurationSection sec = raritySection(rarity);
        if (sec == null || !sec.getBoolean("enabled", true)) return 0;
        return sec.getInt("one-in-chance", 0);
    }

    /** Flat fiber multiplier bonus this rarity grants. */
    public double getFiberBonus(PetRarity rarity) {
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getDouble("fiber-bonus", 0.0) : 0.0;
    }

    /** Display name as configured (uncolored). */
    public String getDisplayName(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return "None";
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getString("display-name", rarity.name()) : rarity.name();
    }

    /** Chat color code string (e.g. "&6" or "&#FFD700"). */
    public String getChatColor(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return "&7";
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getString("chat-color", "&7") : "&7";
    }

    /** Glass pane material for the GUI slot. */
    public Material getGlassMaterial(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return Material.GRAY_STAINED_GLASS_PANE;
        ConfigurationSection sec = raritySection(rarity);
        if (sec == null) return Material.GRAY_STAINED_GLASS_PANE;
        String name = sec.getString("glass-material", "GRAY_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }

    // ── Public API ─────────────────────────────────────────────

    /** Returns the highest pet rarity this player has found. */
    public PetRarity getPlayerPet(UUID uuid) {
        return plugin.getDataManager().getPlayerData(uuid).getPetRarity();
    }

    /** Flat fiber bonus granted by the player's current pet (reads from config). */
    public double getPetFiberBonus(UUID uuid) {
        return getFiberBonus(getPlayerPet(uuid));
    }

    /**
     * Called on every crop break. Rolls for a pet upgrade.
     * Players cannot find pets of equal or lower rarity than what they already own.
     */
    public void rollForPet(Player player) {
        UUID uuid = player.getUniqueId();
        PetRarity current = getPlayerPet(uuid);

        // Start rolling from one tier above the player's current pet
        PetRarity candidate = current.next();

        while (candidate != null) {
            int chance = getOneInChance(candidate);
            if (chance > 0 && random.nextInt(chance) == 0) {
                awardPet(player, candidate);
                return; // Only one pet awarded per crop break
            }
            candidate = candidate.next();
        }
    }

    // ── Internal ───────────────────────────────────────────────

    private void awardPet(Player player, PetRarity rarity) {
        plugin.getDataManager().getPlayerData(player.getUniqueId()).setPetRarity(rarity);
        plugin.getDataManager().saveAsync();

        // Refresh the cosmetic so the new rarity's skull appears immediately
        plugin.getPetCosmeticManager().refresh(player);

        broadcastPetFound(player, rarity);
    }

    private void broadcastPetFound(Player player, PetRarity rarity) {
        String broadcastTemplate = cfg().getString(
                "pets.broadcast-message",
                "&5&l✦ {player} &7found a {rarity} &7Pet! &8({chance} chance)"
        );
        if (broadcastTemplate.isEmpty()) return;

        String color       = getChatColor(rarity);
        String displayName = getDisplayName(rarity);
        String chance      = "1/" + NumberUtil.formatRaw(getOneInChance(rarity));

        String colored = ColorUtil.translate(broadcastTemplate
                .replace("{player}", color + "&l" + player.getName() + "&r")
                .replace("{rarity}", color + "&l" + displayName + "&r")
                .replace("{chance}", chance)
        );
        plugin.getServer().broadcastMessage(colored);
    }
}