package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    // Looked up by both ids, so a crew id from another voyage is never found
    @EntityGraph(attributePaths = "voyage")
    Optional<CrewMember> findWithVoyageByIdAndVoyageId(Long id, String voyageId);
}
