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
}
