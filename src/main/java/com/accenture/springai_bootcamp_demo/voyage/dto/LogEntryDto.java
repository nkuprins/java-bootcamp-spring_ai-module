package com.accenture.springai_bootcamp_demo.voyage.dto;

import com.accenture.springai_bootcamp_demo.voyage.entity.LogKind;
import java.time.Instant;

public record LogEntryDto(
        Long id,
        int turn,
        LogKind kind,
        String text,
        Instant createdAt) {
}
