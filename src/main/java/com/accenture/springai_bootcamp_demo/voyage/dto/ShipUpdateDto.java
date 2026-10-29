package com.accenture.springai_bootcamp_demo.voyage.dto;

import java.util.List;

public record ShipUpdateDto(
        VoyageDto voyage,
        List<LogEntryDto> entries) {
}
