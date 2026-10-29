package com.accenture.springai_bootcamp_demo.voyage.dto;

import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipEvent;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for triggering an event. Location is optional and defaults to
 * the room where the event fits best.
 */
public record ShipEventRequest(
        @NotNull(message = "type must not be null")
        ShipEvent type,
        Location location) {
}
