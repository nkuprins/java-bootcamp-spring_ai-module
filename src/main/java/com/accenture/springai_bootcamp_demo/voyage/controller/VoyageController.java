package com.accenture.springai_bootcamp_demo.voyage.controller;

import com.accenture.springai_bootcamp_demo.voyage.dto.InterviewDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.InterviewRequest;
import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.SendTransmissionRequest;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipEventRequest;
import com.accenture.springai_bootcamp_demo.voyage.dto.ShipUpdateDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.TransmissionDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.service.InterviewService;
import com.accenture.springai_bootcamp_demo.voyage.service.MemoryService;
import com.accenture.springai_bootcamp_demo.voyage.service.ShipSimulation;
import com.accenture.springai_bootcamp_demo.voyage.service.TransmissionService;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    private final MemoryService memoryService;
    private final TransmissionService transmissionService;
    private final InterviewService interviewService;

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

    // Without a query: everything they remember, newest first. With one: what they would recall, best first
    @GetMapping("/{voyageId}/crew/{crewMemberId}/memories")
    public List<MemoryDto> memories(@PathVariable String voyageId,
                                    @PathVariable Long crewMemberId,
                                    @RequestParam(required = false) String query,
                                    @RequestParam(defaultValue = "5") int limit) {
        return memoryService.getMemories(voyageId, crewMemberId, query, limit);
    }

    @PostMapping("/{voyageId}/crew/{crewMemberId}/interview")
    public InterviewDto interview(@PathVariable String voyageId,
                                  @PathVariable Long crewMemberId,
                                  @Valid @RequestBody InterviewRequest request) {
        return interviewService.interview(voyageId, crewMemberId, request.question());
    }

    @PostMapping("/{voyageId}/transmissions")
    public ResponseEntity<TransmissionDto> sendTransmission(@PathVariable String voyageId,
                                                            @Valid @RequestBody SendTransmissionRequest request) {
        TransmissionDto transmission = transmissionService.send(voyageId, request.message());
        return ResponseEntity.created(URI.create("/api/voyages/" + voyageId + "/transmissions/" + transmission.id()))
                .body(transmission);
    }

    @GetMapping("/{voyageId}/transmissions")
    public List<TransmissionDto> transmissions(@PathVariable String voyageId) {
        return transmissionService.getTransmissions(voyageId);
    }
}
