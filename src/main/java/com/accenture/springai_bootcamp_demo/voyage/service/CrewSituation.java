package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipStatus;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import java.util.List;

// What one crew member can see when deciding: read inside a transaction, used after it
record CrewSituation(
        CrewIdentity who,
        Location location,
        List<String> companions,
        String lastWords,
        int turn,
        ShipStatus ship) {

    static CrewSituation of(Voyage voyage, CrewMember member) {
        List<String> companions = voyage.getCrew().stream()
                .filter(other -> other != member && other.getLocation() == member.getLocation())
                .map(other -> other.getName() + " (" + other.getRole().toLowerCase() + ")")
                .toList();
        return new CrewSituation(CrewIdentity.of(member), member.getLocation(), companions,
                member.getLastWords(), voyage.getTurn(), voyage.status());
    }

    boolean isAlone() {
        return companions.isEmpty();
    }

    String company() {
        return isAlone() ? "You are alone." : "With you: " + String.join(", ", companions) + ".";
    }

    boolean wouldRepeat(String message) {
        return lastWords != null && lastWords.equalsIgnoreCase(message.strip());
    }
}
