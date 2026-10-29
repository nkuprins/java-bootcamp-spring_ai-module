package com.accenture.springai_bootcamp_demo.voyage.entity;

// The ship's system levels at one moment; only Voyage.status() creates it, so the four ints can't get mixed up
public record ShipStatus(int oxygen, int power, int hull, int food) {

    public int level(ShipSystem system) {
        return switch (system) {
            case OXYGEN -> oxygen;
            case POWER -> power;
            case HULL -> hull;
            case FOOD -> food;
        };
    }

    public String summary() {
        return "oxygen %d, power %d, hull %d, food %d".formatted(oxygen, power, hull, food);
    }
}
