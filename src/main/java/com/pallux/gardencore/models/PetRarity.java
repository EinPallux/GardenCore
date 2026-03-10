package com.pallux.gardencore.models;

/**
 * Ordered pet rarity tiers.
 * All tuneable values (chance, bonus, display name, color, material) are
 * read from settings/pets.yml via PetManager — nothing is hard-coded here
 * except the internal key used to look up the YAML section.
 */
public enum PetRarity {

    NONE    ("none"),
    COMMON  ("common"),
    UNCOMMON("uncommon"),
    RARE    ("rare"),
    EPIC    ("epic"),
    LEGENDARY("legendary"),
    MYTHIC  ("mythic"),
    DIVINE  ("divine");

    /** Key used in pets.yml under pets.rarities.<key>. NONE has no section. */
    private final String configKey;

    PetRarity(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigKey() { return configKey; }

    /** Returns the rarity one tier above this one, or null if already DIVINE. */
    public PetRarity next() {
        PetRarity[] vals = values();
        int idx = ordinal() + 1;
        return idx < vals.length ? vals[idx] : null;
    }
}