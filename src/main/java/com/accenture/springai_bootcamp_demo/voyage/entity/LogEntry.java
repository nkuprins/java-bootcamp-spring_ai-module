package com.accenture.springai_bootcamp_demo.voyage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

// Not mapped as a collection on Voyage: the log only grows, and loading the ship should not load its whole history
@Entity
@Table(name = "log_entries")
@Getter
@NoArgsConstructor
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @Column(nullable = false)
    private int turn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LogKind kind;

    @Column(nullable = false, length = 2000)
    private String text;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static LogEntry of(Voyage voyage, LogKind kind, String text) {
        LogEntry logEntry = new LogEntry();
        logEntry.voyage = voyage;
        logEntry.turn = voyage.getTurn();
        logEntry.kind = kind;
        logEntry.text = text;
        logEntry.createdAt = Instant.now();
        return logEntry;
    }
}
