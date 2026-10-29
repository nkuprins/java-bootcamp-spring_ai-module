package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipSystem;

// The one thing a crew member chose to do this turn; sealed so every switch over it must handle all of them
sealed interface CrewAction {

    record Move(Location destination) implements CrewAction {
    }

    record Say(String message) implements CrewAction {
    }

    record Announce(String message) implements CrewAction {
    }

    record Repair(ShipSystem system) implements CrewAction {
    }

    record Rest() implements CrewAction {
    }
}
