package com.accenture.springai_bootcamp_demo.voyage.service;

public class CrewMemberNotFoundException extends RuntimeException {

    public CrewMemberNotFoundException(String voyageId, Long crewMemberId) {
        super("Crew member " + crewMemberId + " not found on voyage " + voyageId);
    }
}
