package com.accenture.springai_bootcamp_demo.voyage.dto;

/**
 * Why a memory was recalled: raw relevance (cosine similarity), recency and
 * importance, each 0-1, and the total used for ranking (sum of the three
 * after normalizing across all candidates, so 0-3).
 */
public record MemoryScoreDto(
        double relevance,
        double recency,
        double importance,
        double total) {
}
