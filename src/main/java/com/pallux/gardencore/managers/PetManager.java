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

    private FileConfiguration cfg() {
        return plugin.getConfigManager().getPetsConfig();
    }

    private ConfigurationSection raritySection(PetRarity rarity) {
        return cfg().getConfigurationSection("pets.rarities." + rarity.getConfigKey());
    }

    public int getOneInChance(PetRarity rarity) {
        ConfigurationSection sec = raritySection(rarity);
        if (sec == null || !sec.getBoolean("enabled", true)) return 0;
        return sec.getInt("one-in-chance", 0);
    }

    public double getFiberBonus(PetRarity rarity) {
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getDouble("fiber-bonus", 0.0) : 0.0;
    }

    public String getDisplayName(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return "None";
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getString("display-name", rarity.name()) : rarity.name();
    }

    public String getChatColor(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return "&7";
        ConfigurationSection sec = raritySection(rarity);
        return sec != null ? sec.getString("chat-color", "&7") : "&7";
    }

    public Material getGlassMaterial(PetRarity rarity) {
        if (rarity == PetRarity.NONE) return Material.GRAY_STAINED_GLASS_PANE;
        ConfigurationSection sec = raritySection(rarity);
        if (sec == null) return Material.GRAY_STAINED_GLASS_PANE;
        String name = sec.getString("glass-material", "GRAY_STAINED_GLASS_PANE");
        Material mat = Material.matchMaterial(name);
        return mat != null ? mat : Material.GRAY_STAINED_GLASS_PANE;
    }

    public PetRarity getPlayerPet(UUID uuid) {
        return plugin.getDataManager().getPlayerData(uuid).getPetRarity();
    }

    public double getPetFiberBonus(UUID uuid) {
        return getFiberBonus(getPlayerPet(uuid));
    }

    public void rollForPet(Player player) {
        UUID uuid = player.getUniqueId();
        PetRarity current = getPlayerPet(uuid);

        PetRarity candidate = current.next();

        while (candidate != null) {
            int chance = getOneInChance(candidate);
            if (chance > 0 && random.nextInt(chance) == 0) {
                awardPet(player, candidate);
                return;
            }
            candidate = candidate.next();
        }
    }

    private void awardPet(Player player, PetRarity rarity) {
        plugin.getDataManager().getPlayerData(player.getUniqueId()).setPetRarity(rarity);
        plugin.getDataManager().savePlayerAsync(player.getUniqueId());

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