package com.accenture.springai_bootcamp_demo.voyage.dto;

import com.accenture.springai_bootcamp_demo.voyage.entity.Location;

public record CrewMemberDto(
        Long id,
        String name,
        String role,
        Location location) {
}