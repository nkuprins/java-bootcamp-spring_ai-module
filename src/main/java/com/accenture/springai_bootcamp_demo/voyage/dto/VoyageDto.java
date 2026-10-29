package com.accenture.springai_bootcamp_demo.voyage.dto;

import java.time.Instant;
import java.util.List;

public record VoyageDto(
        String id,
        int turn,
        int oxygen,
        int power,
        int hull,
        int food,
        boolean lost,
        Instant createdAt,
        List<CrewMemberDto> crew) {
}