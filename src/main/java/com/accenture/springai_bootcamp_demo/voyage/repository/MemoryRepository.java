package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.Memory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoryRepository extends JpaRepository<Memory, Long> {

    List<Memory> findByCrewMemberId(Long crewMemberId);

    List<Memory> findByCrewMemberIdOrderByIdDesc(Long crewMemberId);
}
