package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;

record CrewIdentity(Long id, String name, String role, String persona) {

    // Must match the roles in voyage.yaml
    private static final String CAPTAIN_ROLE = "Captain";
    private static final String COMMS_OFFICER_ROLE = "Communications officer";

    static CrewIdentity of(CrewMember member) {
        return new CrewIdentity(member.getId(), member.getName(), member.getRole(), member.getPersona());
    }

    boolean isCaptain() {
        return CAPTAIN_ROLE.equals(role);
    }

    boolean isCommsOfficer() {
        return COMMS_OFFICER_ROLE.equals(role);
    }
}
