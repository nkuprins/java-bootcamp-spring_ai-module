package com.accenture.springai_bootcamp_demo.voyage.dto;

import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;

// score is only present when memories were recalled for a query
public record MemoryDto(
        Long id,
        int turn,
        MemoryKind kind,
        String content,
        MemoryScoreDto score) {
}
