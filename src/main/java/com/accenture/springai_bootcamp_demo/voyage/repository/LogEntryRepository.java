package com.accenture.springai_bootcamp_demo.voyage.repository;

import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    List<LogEntry> findByVoyageIdOrderByIdAsc(String voyageId);
}
