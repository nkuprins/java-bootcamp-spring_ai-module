package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoyageRepository extends JpaRepository<Voyage, String> {

    @EntityGraph(attributePaths = "crew")
    Optional<Voyage> findWithCrewById(String id);
}
