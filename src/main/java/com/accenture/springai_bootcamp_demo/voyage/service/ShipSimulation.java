package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.Location;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.ShipEvent;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.mapper.VoyageMapper;
import com.accenture.springai_bootcamp_demo.voyage.repository.LogEntryRepository;
import com.accenture.springai_bootcamp_demo.voyage.repository.VoyageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Changes the ship: runs turns and applies events. Transactions are opened by
 * hand with {@link TransactionTemplate} so the busy guard wraps the commit, and
 * so later steps can call the model between short transactions instead of
 * inside one long one (SQLite allows only one writer at a time).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipSimulation {
    private final VoyageRepository voyageRepository;
    private final LogEntryRepository logEntryRepository;
    private final VoyageMapper voyageMapper;
    private final TransactionTemplate transactionTemplate;

    // Turns and events change the same ship row, so only one may run per voyage at a time
    private final Set<String> busyVoyages = ConcurrentHashMap.newKeySet();

    public ShipUpdateDto runTurn(String voyageId) {
        return exclusively(voyageId, () -> transactionTemplate.execute(status -> {
            Voyage voyage = loadActiveVoyage(voyageId);
            voyage.advanceTurn();

            List<LogEntry> entries = new ArrayList<>();
            entries.add(LogEntry.of(voyage, LogKind.STATUS, statusReport(voyage)));
            if (voyage.isLost()) {
                entries.add(LogEntry.of(voyage, LogKind.STATUS, "Oxygen is gone. The Long Voyage is lost."));
                log.info("Voyage {} lost on turn {}", voyageId, voyage.getTurn());
            }
            return saveUpdate(voyage, entries);
        }));
    }

    public ShipUpdateDto triggerEvent(String voyageId, ShipEvent event, Location location) {
        Location where = location != null ? location : event.defaultLocation();
        return exclusively(voyageId, () -> transactionTemplate.execute(status -> {
            Voyage voyage = loadActiveVoyage(voyageId);
            voyage.adjust(event.system(), event.impact());

            String text = "%s in %s: %s %+d".formatted(
                    event.label(), where.label(), event.system().label(), event.impact());
            return saveUpdate(voyage, List.of(LogEntry.of(voyage, LogKind.EVENT, text)));
        }));
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

    private <T> T exclusively(String voyageId, Supplier<T> work) {
        if (!busyVoyages.add(voyageId)) {
            throw new VoyageStateException("Voyage " + voyageId + " is busy with another turn or event");
        }
        try {
            return work.get();
        } finally {
            busyVoyages.remove(voyageId);
        }
    }

    private static String statusReport(Voyage voyage) {
        return "Turn %d: oxygen %d, power %d, hull %d, food %d".formatted(
                voyage.getTurn(), voyage.getOxygen(), voyage.getPower(), voyage.getHull(), voyage.getFood());
    }
}
