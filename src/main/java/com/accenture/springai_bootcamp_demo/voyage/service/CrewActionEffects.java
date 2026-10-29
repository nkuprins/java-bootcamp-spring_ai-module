package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Announce;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Move;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Repair;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Rest;
import com.accenture.springai_bootcamp_demo.voyage.service.CrewAction.Say;
import java.util.List;
import java.util.stream.Stream;

/**
 * What a crew member's action does to the ship, the log and everyone's
 * memories. This is where the rules are enforced for real: the tools only
 * checked them against what the crew member could see.
 */
final class CrewActionEffects {

    private CrewActionEffects() {
    }

    static ShipChange apply(Voyage voyage, CrewMember actor, CrewAction action) {
        return switch (action) {
            case Move(var destination) -> {
                actor.setLocation(destination);
                yield logOnly(voyage, "%s went to %s.".formatted(actor.getName(), destination.label()));
            }
            case Say(var message) -> {
                actor.setLastWords(message);
                yield say(voyage, actor, message);
            }
            case Announce(var message) -> {
                actor.setLastWords(message);
                yield announce(voyage, actor, message);
            }
            case Repair(var system) -> {
                voyage.repair(system);
                yield new ShipChange(
                        List.of(LogEntry.of(voyage, LogKind.ACTION, "%s repaired the %s: %s +%d"
                                .formatted(actor.getName(), system.label(), system.label(), Voyage.REPAIR_AMOUNT))),
                        List.of(memory(voyage, actor, MemoryKind.ACTION,
                                "I spent the hour repairing the %s in %s.".formatted(system.label(), actor.getLocation().label()))));
            }
            case Rest() -> logOnly(voyage, actor.getName() + " rested.");
        };
    }

    static ShipChange hesitate(Voyage voyage, CrewMember actor) {
        return logOnly(voyage, actor.getName() + " hesitated and did nothing.");
    }

    private static ShipChange say(Voyage voyage, CrewMember speaker, String message) {
        List<CrewMember> listeners = voyage.getCrew().stream()
                .filter(other -> other != speaker && other.getLocation() == speaker.getLocation())
                .toList();
        String room = speaker.getLocation().label();
        List<String> names = listeners.stream().map(CrewMember::getName).toList();

        List<MemoryDraft> memories = Stream.concat(
                Stream.of(memory(voyage, speaker, MemoryKind.SPOKE,
                        "I said to %s in %s: \"%s\"".formatted(String.join(", ", names), room, message))),
                listeners.stream().map(listener -> memory(voyage, listener, MemoryKind.CONVERSATION,
                        "%s said to me in %s: \"%s\"".formatted(speaker.getName(), room, message))))
                .toList();

        String text = "%s to %s in %s: \"%s\"".formatted(speaker.getName(), String.join(", ", names), room, message);
        return new ShipChange(List.of(LogEntry.of(voyage, LogKind.ACTION, text)), memories);
    }

    private static ShipChange announce(Voyage voyage, CrewMember captain, String message) {
        List<MemoryDraft> memories = Stream.concat(
                Stream.of(memory(voyage, captain, MemoryKind.SPOKE,
                        "I announced to the whole crew over the intercom: \"%s\"".formatted(message))),
                voyage.getCrew().stream()
                        .filter(other -> other != captain)
                        .map(listener -> memory(voyage, listener, MemoryKind.ANNOUNCEMENT,
                                "Captain %s announced over the intercom: \"%s\"".formatted(captain.getName(), message))))
                .toList();

        String text = "%s over the intercom: \"%s\"".formatted(captain.getName(), message);
        return new ShipChange(List.of(LogEntry.of(voyage, LogKind.ACTION, text)), memories);
    }

    private static ShipChange logOnly(Voyage voyage, String text) {
        return new ShipChange(List.of(LogEntry.of(voyage, LogKind.ACTION, text)), List.of());
    }

    private static MemoryDraft memory(Voyage voyage, CrewMember member, MemoryKind kind, String content) {
        return new MemoryDraft(member.getId(), voyage.getTurn(), kind, content);
    }
}
