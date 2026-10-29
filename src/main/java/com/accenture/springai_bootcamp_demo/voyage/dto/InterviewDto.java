package com.accenture.springai_bootcamp_demo.voyage.dto;

public record InterviewDto(
        Long crewMemberId,
        String name,
        int turn,
        String question,
        String answer) {
}
