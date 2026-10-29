package com.accenture.springai_bootcamp_demo.voyage.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

@Entity
@Table(name = "memories")
@Getter
@NoArgsConstructor
public class Memory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crew_member_id", nullable = false)
    private CrewMember crewMember;

    @Column(nullable = false)
    private int turn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemoryKind kind;

    @Column(nullable = false, length = 2000)
    private String content;

    @Convert(converter = EmbeddingConverter.class)
    @Column(nullable = false)
    private float[] embedding;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Memory of(CrewMember crewMember, int turn, MemoryKind kind, String content, float[] embedding) {
        Memory memory = new Memory();
        memory.crewMember = crewMember;
        memory.turn = turn;
        memory.kind = kind;
        memory.content = content;
        memory.embedding = embedding;
        memory.createdAt = Instant.now();
        return memory;
    }
}
