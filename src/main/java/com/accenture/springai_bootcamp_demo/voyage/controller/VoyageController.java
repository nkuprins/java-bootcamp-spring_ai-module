package com.accenture.springai_bootcamp_demo.voyage.controller;

import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipEventRequest;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.service.ShipSimulation;
import com.accenture.springai_bootcamp_demo.voyage.service.VoyageService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API for Mission Control: launch a voyage and watch the ship.
 */
@RestController
@RequestMapping("/api/voyages")
@RequiredArgsConstructor
public class VoyageController {

    private final VoyageService voyageService;
    private final ShipSimulation shipSimulation;

    @PostMapping
    public ResponseEntity<VoyageDto> create() {
        VoyageDto voyage = voyageService.createVoyage();
        return ResponseEntity.created(URI.create("/api/voyages/" + voyage.id())).body(voyage);
    }

    @GetMapping("/{voyageId}")
    public VoyageDto get(@PathVariable String voyageId) {
        return voyageService.getVoyage(voyageId);
    }

    @PostMapping("/{voyageId}/turns")
    public ShipUpdateDto runTurn(@PathVariable String voyageId) {
        return shipSimulation.runTurn(voyageId);
    }

    @PostMapping("/{voyageId}/events")
    public ShipUpdateDto triggerEvent(@PathVariable String voyageId,
                                      @Valid @RequestBody ShipEventRequest request) {
        return shipSimulation.triggerEvent(voyageId, request.type(), request.location());
    }

    @GetMapping("/{voyageId}/log")
    public List<LogEntryDto> log(@PathVariable String voyageId) {
        return voyageService.getLog(voyageId);
    }
}
