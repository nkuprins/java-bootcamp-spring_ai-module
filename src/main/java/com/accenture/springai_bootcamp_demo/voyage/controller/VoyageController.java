package com.accenture.springai_bootcamp_demo.voyage.controller;

import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.service.VoyageService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    public ResponseEntity<VoyageDto> create() {
        VoyageDto voyage = voyageService.createVoyage();
        return ResponseEntity.created(URI.create("/api/voyages/" + voyage.id())).body(voyage);
    }

    @GetMapping("/{voyageId}")
    public VoyageDto get(@PathVariable String voyageId) {
        return voyageService.getVoyage(voyageId);
    }
}
