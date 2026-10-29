package com.accenture.springai_bootcamp_demo.voyage.entity;

public enum ShipSystem {
    OXYGEN,
    POWER,
    HULL,
    FOOD;

    public String label() {
        return name().toLowerCase();
    }
}
