package com.accenture.springai_bootcamp_demo.voyage.entity;

public enum ShipSystem {
    OXYGEN(Location.HYDROPONICS),
    POWER(Location.ENGINEERING),
    HULL(Location.ENGINEERING),
    FOOD(Location.HYDROPONICS);

    private final Location repairLocation;

    ShipSystem(Location repairLocation) {
        this.repairLocation = repairLocation;
    }

    public String label() {
        return name().toLowerCase();
    }

    public Location repairLocation() {
        return repairLocation;
    }
}
