package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Changes the ship: runs turns and applies events. Transactions are opened by
 * hand with {@link TransactionTemplate} so the busy guard wraps the commit, and
 * so model calls (embeddings now, crew decisions later) run between short
 * transactions instead of inside one long one (SQLite allows only one writer
 * at a time).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipSimulation {
    private final VoyageRepository voyageRepository;
    private final LogEntryRepository logEntryRepository;
    private final MemoryService memoryService;
    private final VoyageMapper voyageMapper;
    private final TransactionTemplate transactionTemplate;

    // Turns and events change the same ship row, so only one may run per voyage at a time
    private final Set<String> busyVoyages = ConcurrentHashMap.newKeySet();

    private record Outcome(ShipUpdateDto update, List<MemoryDraft> memories) {
    }

    public ShipUpdateDto runTurn(String voyageId) {
        return changeShip(voyageId, voyage -> {
            voyage.advanceTurn();

            List<LogEntry> entries = new ArrayList<>();
            entries.add(LogEntry.of(voyage, LogKind.STATUS, statusReport(voyage)));
            if (voyage.isLost()) {
                entries.add(LogEntry.of(voyage, LogKind.STATUS, "Oxygen is gone. The Long Voyage is lost."));
                log.info("Voyage {} lost on turn {}", voyageId, voyage.getTurn());
            }
            List<MemoryDraft> memories = voyage.getCrew().stream()
                    .flatMap(member -> routineMemory(voyage, member).stream())
                    .toList();
            return new Outcome(saveUpdate(voyage, entries), memories);
        });
    }

    public ShipUpdateDto triggerEvent(String voyageId, ShipEvent event, Location location) {
        Location where = location != null ? location : event.defaultLocation();
        return changeShip(voyageId, voyage -> {
            voyage.adjust(event.system(), event.impact());

            String text = "%s in %s: %s %+d".formatted(
                    event.label(), where.label(), event.system().label(), event.impact());
            List<MemoryDraft> memories = voyage.getCrew().stream()
                    .map(member -> eventMemory(voyage, member, event, where))
                    .toList();
            return new Outcome(saveUpdate(voyage, List.of(LogEntry.of(voyage, LogKind.EVENT, text))), memories);
        });
    }

    // Order matters: mark busy -> change the ship and commit -> embed and save memories -> mark free
    private ShipUpdateDto changeShip(String voyageId, Function<Voyage, Outcome> change) {
        if (!busyVoyages.add(voyageId)) {
            throw new VoyageStateException("Voyage " + voyageId + " is busy with another turn or event");
        }
        try {
            Outcome outcome = transactionTemplate.execute(status -> change.apply(loadActiveVoyage(voyageId)));
            memoryService.recordAll(outcome.memories());
            return outcome.update();
        } finally {
            busyVoyages.remove(voyageId);
        }
    }

    private ShipUpdateDto saveUpdate(Voyage voyage, List<LogEntry> entries) {
        logEntryRepository.saveAll(entries);
        return new ShipUpdateDto(voyageMapper.toDto(voyage), voyageMapper.toLogDtos(entries));
    }

    private Voyage loadActiveVoyage(String voyageId) {
        Voyage voyage = voyageRepository.findWithCrewById(voyageId)
                .orElseThrow(() -> new VoyageNotFoundException(voyageId));
        if (voyage.isLost()) {
            throw new VoyageStateException("Voyage " + voyageId + " is lost");
        }
        return voyage;
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
        return "Turn %d: oxygen %d, power %d, hull %d, food %d".formatted(
                voyage.getTurn(), voyage.getOxygen(), voyage.getPower(), voyage.getHull(), voyage.getFood());
    }
}
