package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    List<LogEntry> findByVoyageIdOrderByIdAsc(String voyageId);

    Optional<LogEntry> findTopByVoyageIdOrderByIdDesc(String voyageId);

    void deleteByVoyageIdAndIdGreaterThan(String voyageId, Long id);
}
