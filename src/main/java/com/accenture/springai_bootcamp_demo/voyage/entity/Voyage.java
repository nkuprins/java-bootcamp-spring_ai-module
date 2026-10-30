package com.accenture.springai_bootcamp_demo.voyage.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "voyages")
@Getter
@NoArgsConstructor
public class Voyage {

    // The ship starts worn down, so there is pressure from the first turn
    private static final int STARTING_OXYGEN = 80;
    private static final int STARTING_POWER = 90;
    private static final int STARTING_HULL = 95;
    private static final int STARTING_FOOD = 70;

    private static final int MIN_LEVEL = 0;
    public static final int MAX_LEVEL = 100;

    // Breathing uses oxygen every turn; a damaged hull leaks it and weak power starves the scrubbers
    private static final int OXYGEN_USE_PER_TURN = 2;
    private static final int LEAK_BELOW_HULL = 50;
    private static final int LEAK_OXYGEN_LOSS = 3;
    private static final int WEAK_SCRUBBERS_BELOW_POWER = 30;
    private static final int WEAK_SCRUBBERS_OXYGEN_LOSS = 2;
    private static final int FOOD_USE_PER_TURN = 1;
    private static final int POWER_USE_PER_TURN = 1;
    public static final int REPAIR_AMOUNT = 5;

    // UUID kept as String: SQLite has no uuid type
    // Long ids appear in URLs, so they don't suit
    @Id
    @Column(nullable = false, updatable = false)
    private String id;

    @Column(nullable = false)
    private int turn;

    @Column(nullable = false)
    private int oxygen;

    @Column(nullable = false)
    private int power;

    @Column(nullable = false)
    private int hull;

    @Column(nullable = false)
    private int food;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "voyage", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<CrewMember> crew = new ArrayList<>();

    public static Voyage create() {
        Voyage voyage = new Voyage();
        voyage.id = UUID.randomUUID().toString();
        voyage.oxygen = STARTING_OXYGEN;
        voyage.power = STARTING_POWER;
        voyage.hull = STARTING_HULL;
        voyage.food = STARTING_FOOD;
        voyage.createdAt = Instant.now();
        return voyage;
    }

    public void addCrewMember(CrewMember crewMember) {
        crewMember.setVoyage(this);
        this.crew.add(crewMember);
    }

    public void advanceTurn() {
        turn++;
        int oxygenLoss = OXYGEN_USE_PER_TURN;
        if (hull < LEAK_BELOW_HULL) {
            oxygenLoss += LEAK_OXYGEN_LOSS;
        }
        if (power < WEAK_SCRUBBERS_BELOW_POWER) {
            oxygenLoss += WEAK_SCRUBBERS_OXYGEN_LOSS;
        }
        adjust(ShipSystem.OXYGEN, -oxygenLoss);
        adjust(ShipSystem.FOOD, -FOOD_USE_PER_TURN);
        adjust(ShipSystem.POWER, -POWER_USE_PER_TURN);
    }

    public void repair(ShipSystem system) {
        adjust(system, REPAIR_AMOUNT);
    }

    public void adjust(ShipSystem system, int delta) {
        switch (system) {
            case OXYGEN -> oxygen = clampLevel(oxygen + delta);
            case POWER -> power = clampLevel(power + delta);
            case HULL -> hull = clampLevel(hull + delta);
            case FOOD -> food = clampLevel(food + delta);
        }
    }

    // Puts the ship back to an earlier moment, when a turn or event that changed it failed halfway
    public void restore(int turn, ShipStatus status) {
        this.turn = turn;
        oxygen = status.oxygen();
        power = status.power();
        hull = status.hull();
        food = status.food();
    }

    public boolean isLost() {
        return oxygen == MIN_LEVEL;
    }

    public ShipStatus status() {
        return new ShipStatus(oxygen, power, hull, food);
    }

    private static int clampLevel(int level) {
        return Math.clamp(level, MIN_LEVEL, MAX_LEVEL);
    }
}
