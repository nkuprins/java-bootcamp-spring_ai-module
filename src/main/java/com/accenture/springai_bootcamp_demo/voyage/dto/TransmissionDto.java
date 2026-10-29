package com.accenture.springai_bootcamp_demo.voyage.dto;

import java.time.Instant;

public record TransmissionDto(
        Long id,
        String message,
        int sentTurn,
        int arrivalTurn,
        boolean delivered,
        Instant createdAt) {
}
