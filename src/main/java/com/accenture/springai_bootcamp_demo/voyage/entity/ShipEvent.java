package com.accenture.springai_bootcamp_demo.voyage.entity;

/**
 * Something Mission Control can make happen on the ship. Each event hits one
 * system and, unless told otherwise, happens in the room where it fits best.
 */
public enum ShipEvent {
    HULL_BREACH("Hull breach", ShipSystem.HULL, -20, Location.ENGINEERING),
    POWER_FAILURE("Power failure", ShipSystem.POWER, -25, Location.ENGINEERING),
    CROP_BLIGHT("Crop blight", ShipSystem.FOOD, -20, Location.HYDROPONICS),
    STRANGE_SIGNAL("Strange signal", ShipSystem.POWER, -5, Location.COMMS_ARRAY);

    private final String label;
    private final ShipSystem system;
    private final int impact;
    private final Location defaultLocation;

    ShipEvent(String label, ShipSystem system, int impact, Location defaultLocation) {
        this.label = label;
        this.system = system;
        this.impact = impact;
        this.defaultLocation = defaultLocation;
    }

    public String label() {
        return label;
    }

    public ShipSystem system() {
        return system;
    }

    public int impact() {
        return impact;
    }

    public Location defaultLocation() {
        return defaultLocation;
    }
}
