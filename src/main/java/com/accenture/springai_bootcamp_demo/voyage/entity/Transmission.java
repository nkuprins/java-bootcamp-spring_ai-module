package com.accenture.springai_bootcamp_demo.voyage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transmissions")
@Getter
@NoArgsConstructor
public class Transmission {

    // Earth is so far away that a message needs this many hours to reach the ship
    public static final int DELAY_IN_TURNS = 4;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private int sentTurn;

    @Column(nullable = false)
    private int arrivalTurn;

    @Column(nullable = false)
    private boolean delivered;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Transmission send(Voyage voyage, String message) {
        Transmission transmission = new Transmission();
        transmission.voyage = voyage;
        transmission.message = message;
        transmission.sentTurn = voyage.getTurn();
        transmission.arrivalTurn = voyage.getTurn() + DELAY_IN_TURNS;
        transmission.createdAt = Instant.now();
        return transmission;
    }

    public void markDelivered() {
        this.delivered = true;
    }

    public void markUndelivered() {
        this.delivered = false;
    }
}
