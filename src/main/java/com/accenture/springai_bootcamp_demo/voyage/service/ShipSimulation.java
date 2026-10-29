package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.CrewMemberDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipEvent;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.mapper.VoyageMapper;
import com.accenture.springai_bootcamp_demo.voyage.repository.LogEntryRepository;
import com.accenture.springai_bootcamp_demo.voyage.repository.VoyageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Changes the ship: runs turns and applies events. Transactions are opened by
 * hand with {@link TransactionTemplate} so the busy guard wraps the commit, and
 * so model calls (crew decisions, embeddings) run between short transactions
 * instead of inside one long one (SQLite allows only one writer at a time).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipSimulation {
    private final VoyageRepository voyageRepository;
    private final LogEntryRepository logEntryRepository;
    private final MemoryService memoryService;
    private final CrewAgent crewAgent;
    private final Reflector reflector;
    private final Narrator narrator;
    private final VoyageMapper voyageMapper;
    private final TransactionTemplate transactionTemplate;

    // Every 4th hour people step back and draw conclusions; more often would mostly repeat them
    private static final int REFLECTION_INTERVAL = 4;

    // Turns and events change the same ship row, so only one may run per voyage at a time
    private final Set<String> busyVoyages = ConcurrentHashMap.newKeySet();

    private record Outcome(ShipUpdateDto update, List<MemoryDraft> memories) {
    }

    public ShipUpdateDto runTurn(String voyageId) {
        return whileBusy(voyageId, () -> {
            ShipUpdateDto update = applyChange(voyageId, this::advanceTurn);
            if (update.voyage().lost()) {
                return update;
            }
            int turn = update.voyage().turn();
            List<Long> crewIds = update.voyage().crew().stream().map(CrewMemberDto::id).toList();
            List<LogEntryDto> entries = new ArrayList<>(update.entries());

            // Crew act one after another, so each sees what the people before them did
            for (Long crewMemberId : crewIds) {
                entries.addAll(crewMemberActs(voyageId, crewMemberId));
            }
            applyChange(voyageId, voyage -> new ShipChange(List.of(), routineMemories(voyage)));
            if (turn % REFLECTION_INTERVAL == 0) {
                for (Long crewMemberId : crewIds) {
                    entries.addAll(crewMemberReflects(voyageId, crewMemberId));
                }
            }
            entries.addAll(narrate(voyageId, turn, entries));
            return new ShipUpdateDto(currentVoyage(voyageId), entries);
        });
    }

    public ShipUpdateDto triggerEvent(String voyageId, ShipEvent event, Location location) {
        Location where = location != null ? location : event.defaultLocation();
        return whileBusy(voyageId, () -> applyChange(voyageId, voyage -> {
            voyage.adjust(event.system(), event.impact());

            String text = "%s in %s: %s %+d".formatted(
                    event.label(), where.label(), event.system().label(), event.impact());
            List<MemoryDraft> memories = voyage.getCrew().stream()
                    .map(member -> eventMemory(voyage, member, event, where))
                    .toList();
            return new ShipChange(List.of(LogEntry.of(voyage, LogKind.EVENT, text)), memories);
        }));
    }

    private List<LogEntryDto> crewMemberActs(String voyageId, Long crewMemberId) {
        // The slow model call happens here, with no transaction open
        Optional<CrewAction> action = crewAgent.decide(situationOf(voyageId, crewMemberId));
        return applyChange(voyageId, voyage -> {
            CrewMember actor = crewMember(voyage, crewMemberId);
            return action.map(chosen -> CrewActionEffects.apply(voyage, actor, chosen))
                    .orElseGet(() -> CrewActionEffects.hesitate(voyage, actor));
        }).entries();
    }

    private List<LogEntryDto> crewMemberReflects(String voyageId, Long crewMemberId) {
        List<String> insights = reflector.reflect(situationOf(voyageId, crewMemberId));
        if (insights.isEmpty()) {
            return List.of();
        }
        return applyChange(voyageId, voyage -> reflections(voyage, crewMember(voyage, crewMemberId), insights)).entries();
    }

    private List<LogEntryDto> narrate(String voyageId, int turn, List<LogEntryDto> entries) {
        // Thoughts are private: the official log only knows what people did and what happened to the ship
        List<String> happenings = entries.stream()
                .filter(entry -> entry.kind() != LogKind.THOUGHT)
                .map(LogEntryDto::text)
                .toList();
        return narrator.narrate(turn, happenings)
                .map(text -> applyChange(voyageId, voyage ->
                        new ShipChange(List.of(LogEntry.of(voyage, LogKind.NARRATION, text)), List.of())).entries())
                .orElse(List.of());
    }

    private CrewSituation situationOf(String voyageId, Long crewMemberId) {
        return transactionTemplate.execute(status -> {
            Voyage voyage = loadActiveVoyage(voyageId);
            return CrewSituation.of(voyage, crewMember(voyage, crewMemberId));
        });
    }

    private VoyageDto currentVoyage(String voyageId) {
        return transactionTemplate.execute(status -> voyageMapper.toDto(loadActiveVoyage(voyageId)));
    }

    private ShipChange advanceTurn(Voyage voyage) {
        voyage.advanceTurn();
        List<LogEntry> entries = new ArrayList<>();
        entries.add(LogEntry.of(voyage, LogKind.STATUS, statusReport(voyage)));
        if (voyage.isLost()) {
            entries.add(LogEntry.of(voyage, LogKind.STATUS, "Oxygen is gone. The Long Voyage is lost."));
            log.info("Voyage {} lost on turn {}", voyage.getId(), voyage.getTurn());
        }
        return new ShipChange(entries, List.of());
    }

    private <T> T whileBusy(String voyageId, Supplier<T> work) {
        if (!busyVoyages.add(voyageId)) {
            throw new VoyageStateException("Voyage " + voyageId + " is busy with another turn or event");
        }
        try {
            return work.get();
        } finally {
            busyVoyages.remove(voyageId);
        }
    }

    // One short transaction per change; its memories are embedded and saved after it commits
    private ShipUpdateDto applyChange(String voyageId, Function<Voyage, ShipChange> change) {
        Outcome outcome = transactionTemplate.execute(status -> {
            Voyage voyage = loadActiveVoyage(voyageId);
            ShipChange shipChange = change.apply(voyage);
            logEntryRepository.saveAll(shipChange.entries());
            ShipUpdateDto update = new ShipUpdateDto(voyageMapper.toDto(voyage), voyageMapper.toLogDtos(shipChange.entries()));
            return new Outcome(update, shipChange.memories());
        });
        memoryService.recordAll(outcome.memories());
        return outcome.update();
    }

    private Voyage loadActiveVoyage(String voyageId) {
        Voyage voyage = voyageRepository.findWithCrewById(voyageId)
                .orElseThrow(() -> new VoyageNotFoundException(voyageId));
        if (voyage.isLost()) {
            throw new VoyageStateException("Voyage " + voyageId + " is lost");
        }
        return voyage;
    }

    private static CrewMember crewMember(Voyage voyage, Long crewMemberId) {
        return voyage.getCrew().stream()
                .filter(member -> member.getId().equals(crewMemberId))
                .findFirst()
                .orElseThrow();
    }

    private static ShipChange reflections(Voyage voyage, CrewMember member, List<String> insights) {
        List<LogEntry> entries = insights.stream()
                .map(insight -> LogEntry.of(voyage, LogKind.THOUGHT, member.getName() + " thinks: " + insight))
                .toList();
        List<MemoryDraft> memories = insights.stream()
                .map(insight -> new MemoryDraft(member.getId(), voyage.getTurn(), MemoryKind.REFLECTION, insight))
                .toList();
        return new ShipChange(entries, memories);
    }

    private static List<MemoryDraft> routineMemories(Voyage voyage) {
        return voyage.getCrew().stream()
                .flatMap(member -> routineMemory(voyage, member).stream())
                .toList();
    }

    // An hour alone is not worth remembering, and hundreds of such lines would crowd out real memories
    private static Optional<MemoryDraft> routineMemory(Voyage voyage, CrewMember member) {
        List<String> company = voyage.getCrew().stream()
                .filter(other -> other != member && other.getLocation() == member.getLocation())
                .map(CrewMember::getName)
                .toList();
        if (company.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new MemoryDraft(member.getId(), voyage.getTurn(), MemoryKind.ROUTINE,
                "I spent the hour in %s with %s.".formatted(member.getLocation().label(), String.join(", ", company))));
    }

    // Both texts name the damaged system, so a question like "do we have food?" can find them
    private static MemoryDraft eventMemory(Voyage voyage, CrewMember member, ShipEvent event, Location where) {
        String what = event.label().toLowerCase();
        String damage = "The ship's %s took a hit.".formatted(event.system().label());
        if (member.getLocation() == where) {
            return new MemoryDraft(member.getId(), voyage.getTurn(), MemoryKind.WITNESSED,
                    "I saw a %s happen right in front of me in %s. %s".formatted(what, where.label(), damage));
        }
        return new MemoryDraft(member.getId(), voyage.getTurn(), MemoryKind.ALARM,
                "Alarms sounded: a %s was reported in %s. %s".formatted(what, where.label(), damage));
    }

    private static String statusReport(Voyage voyage) {
        return "Turn %d: %s".formatted(voyage.getTurn(), voyage.status().summary());
    }
}
