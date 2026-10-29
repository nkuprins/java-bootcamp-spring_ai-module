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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "crew_members")
@Getter
@Setter
@NoArgsConstructor
public class CrewMember {

    // Long is enough: crew ids are only used together with their voyage's UUID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voyage_id", nullable = false)
    private Voyage voyage;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, length = 2000)
    private String persona;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Location location;

    public static CrewMember of(String name, String role, String persona, Location location) {
        CrewMember crewMember = new CrewMember();
        crewMember.name = name;
        crewMember.role = role;
        crewMember.persona = persona;
        crewMember.location = location;
        return crewMember;
    }
}
