package com.pallux.gardencore.models;

public class EventData {

    public enum EventType {
        FIBER_AMOUNT,
        MATERIAL_AMOUNT,
        MATERIAL_CHANCE,
        XP_AMOUNT
    }

    private final String key;
    private final String displayName;
    private final EventType type;
    private final double value;

    public EventData(String key, String displayName, EventType type, double value) {
        this.key = key;
        this.displayName = displayName;
        this.type = type;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public EventType getType() { return type; }
    public double getValue() { return value; }
}