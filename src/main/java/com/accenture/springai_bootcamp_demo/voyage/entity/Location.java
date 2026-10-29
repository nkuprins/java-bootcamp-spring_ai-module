package com.accenture.springai_bootcamp_demo.voyage.entity;

/**
 * A room on the ship.
 * Crew members can only see and talk to people in the same room.
 */
public enum Location {
    BRIDGE("the bridge"),
    ENGINEERING("engineering"),
    HYDROPONICS("the hydroponics bay"),
    MEDBAY("the medbay"),
    COMMS_ARRAY("the comms array"),
    MESS_HALL("the mess hall"),
    QUARTERS("the crew quarters");

    private final String label;

    Location(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
