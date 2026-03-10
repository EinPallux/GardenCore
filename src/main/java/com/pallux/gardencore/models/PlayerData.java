package com.pallux.gardencore.models;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private double fiber;
    private double xp;
    private int level;

    private int fiberAmountUpgrade;
    private int materialAmountUpgrade;
    private int materialChanceUpgrade;
    private int cropCooldownUpgrade;

    private double bonusFiberMultiplier;
    private double bonusMaterialAmountMultiplier;
    private double bonusMaterialChanceMultiplier;

    private double driftwood;
    private double moss;
    private double reed;
    private double clover;

    // Research
    private int completedResearches;
    private int activeResearchIndex;
    private long activeResearchStart;

    // Elder perks
    private int elderFiberLevel;
    private int elderMaterialAmountLevel;
    private int elderXpGainLevel;
    private int elderMaterialChanceLevel;

    // Pets
    private PetRarity petRarity;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.fiber = 0;
        this.xp = 0;
        this.level = 1;
        this.fiberAmountUpgrade = 0;
        this.materialAmountUpgrade = 0;
        this.materialChanceUpgrade = 0;
        this.cropCooldownUpgrade = 0;
        this.bonusFiberMultiplier = 0;
        this.bonusMaterialAmountMultiplier = 0;
        this.bonusMaterialChanceMultiplier = 0;
        this.driftwood = 0;
        this.moss = 0;
        this.reed = 0;
        this.clover = 0;
        this.completedResearches = 0;
        this.activeResearchIndex = -1;
        this.activeResearchStart = 0;
        this.elderFiberLevel = 0;
        this.elderMaterialAmountLevel = 0;
        this.elderXpGainLevel = 0;
        this.elderMaterialChanceLevel = 0;
        this.petRarity = PetRarity.NONE;
    }

    public UUID getUuid() { return uuid; }

    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { this.fiber = Math.max(0, fiber); }
    public void addFiber(double amount) { this.fiber += amount; }
    public void takeFiber(double amount) { this.fiber = Math.max(0, fiber - amount); }

    public double getXp() { return xp; }
    public void setXp(double xp) { this.xp = xp; }
    public void addXp(double amount) { this.xp += amount; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getFiberAmountUpgrade() { return fiberAmountUpgrade; }
    public void setFiberAmountUpgrade(int v) { this.fiberAmountUpgrade = v; }

    public int getMaterialAmountUpgrade() { return materialAmountUpgrade; }
    public void setMaterialAmountUpgrade(int v) { this.materialAmountUpgrade = v; }

    public int getMaterialChanceUpgrade() { return materialChanceUpgrade; }
    public void setMaterialChanceUpgrade(int v) { this.materialChanceUpgrade = v; }

    public int getCropCooldownUpgrade() { return cropCooldownUpgrade; }
    public void setCropCooldownUpgrade(int v) { this.cropCooldownUpgrade = v; }

    public double getBonusFiberMultiplier() { return bonusFiberMultiplier; }
    public void setBonusFiberMultiplier(double v) { this.bonusFiberMultiplier = v; }
    public void addBonusFiberMultiplier(double v) { this.bonusFiberMultiplier += v; }

    public double getBonusMaterialAmountMultiplier() { return bonusMaterialAmountMultiplier; }
    public void setBonusMaterialAmountMultiplier(double v) { this.bonusMaterialAmountMultiplier = v; }
    public void addBonusMaterialAmountMultiplier(double v) { this.bonusMaterialAmountMultiplier += v; }

    public double getBonusMaterialChanceMultiplier() { return bonusMaterialChanceMultiplier; }
    public void setBonusMaterialChanceMultiplier(double v) { this.bonusMaterialChanceMultiplier = v; }
    public void addBonusMaterialChanceMultiplier(double v) { this.bonusMaterialChanceMultiplier += v; }

    public double getDriftwood() { return driftwood; }
    public void setDriftwood(double v) { this.driftwood = v; }
    public void addDriftwood(double v) { this.driftwood += v; }

    public double getMoss() { return moss; }
    public void setMoss(double v) { this.moss = v; }
    public void addMoss(double v) { this.moss += v; }

    public double getReed() { return reed; }
    public void setReed(double v) { this.reed = v; }
    public void addReed(double v) { this.reed += v; }

    public double getClover() { return clover; }
    public void setClover(double v) { this.clover = v; }
    public void addClover(double v) { this.clover += v; }

    public int getCompletedResearches() { return completedResearches; }
    public void setCompletedResearches(int v) { this.completedResearches = v; }

    public int getActiveResearchIndex() { return activeResearchIndex; }
    public void setActiveResearchIndex(int v) { this.activeResearchIndex = v; }

    public long getActiveResearchStart() { return activeResearchStart; }
    public void setActiveResearchStart(long v) { this.activeResearchStart = v; }

    public boolean hasActiveResearch() { return activeResearchIndex >= 0; }

    // Elder perk getters/setters
    public int getElderFiberLevel() { return elderFiberLevel; }
    public void setElderFiberLevel(int v) { this.elderFiberLevel = v; }

    public int getElderMaterialAmountLevel() { return elderMaterialAmountLevel; }
    public void setElderMaterialAmountLevel(int v) { this.elderMaterialAmountLevel = v; }

    public int getElderXpGainLevel() { return elderXpGainLevel; }
    public void setElderXpGainLevel(int v) { this.elderXpGainLevel = v; }

    public int getElderMaterialChanceLevel() { return elderMaterialChanceLevel; }
    public void setElderMaterialChanceLevel(int v) { this.elderMaterialChanceLevel = v; }

    // Pet getters/setters
    public PetRarity getPetRarity() { return petRarity != null ? petRarity : PetRarity.NONE; }
    public void setPetRarity(PetRarity rarity) { this.petRarity = rarity; }

    // ── Reset helpers ──────────────────────────────────────────

    public void resetUpgrades() {
        fiberAmountUpgrade = 0;
        materialAmountUpgrade = 0;
        materialChanceUpgrade = 0;
        cropCooldownUpgrade = 0;
    }

    public void resetFiber() {
        fiber = 0;
    }

    public void resetMaterials() {
        driftwood = 0;
        moss = 0;
        reed = 0;
        clover = 0;
    }

    public void resetResearch() {
        completedResearches = 0;
        activeResearchIndex = -1;
        activeResearchStart = 0;
    }

    public void resetElder() {
        elderFiberLevel = 0;
        elderMaterialAmountLevel = 0;
        elderXpGainLevel = 0;
        elderMaterialChanceLevel = 0;
    }

    public void resetPet() {
        petRarity = PetRarity.NONE;
    }

    public void resetAll() {
        fiber = 0;
        xp = 0;
        level = 1;
        resetUpgrades();
        bonusFiberMultiplier = 0;
        bonusMaterialAmountMultiplier = 0;
        bonusMaterialChanceMultiplier = 0;
        resetMaterials();
        resetResearch();
        resetElder();
        resetPet();
    }
}