package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.TransmissionDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.Transmission;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.mapper.VoyageMapper;
import com.accenture.springai_bootcamp_demo.voyage.repository.TransmissionRepository;
import com.accenture.springai_bootcamp_demo.voyage.repository.VoyageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Messages from Mission Control. Sending only queues the message; the turn
 * engine delivers it to the comms officer once it has crossed the distance.
 * Not behind the busy guard: queuing a row does not touch the ship.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransmissionService {
    private final VoyageRepository voyageRepository;
    private final TransmissionRepository transmissionRepository;
    private final VoyageMapper voyageMapper;

    @Transactional
    public TransmissionDto send(String voyageId, String message) {
        Voyage voyage = voyageRepository.findById(voyageId)
                .orElseThrow(() -> new VoyageNotFoundException(voyageId));
        if (voyage.isLost()) {
            throw new VoyageStateException("Voyage " + voyageId + " is lost; nobody is left to receive it");
        }
        Transmission transmission = transmissionRepository.save(Transmission.send(voyage, message.strip()));
        log.info("Transmission {} to voyage {} will arrive on turn {}",
                transmission.getId(), voyageId, transmission.getArrivalTurn());
        return voyageMapper.toDto(transmission);
    }

    @Transactional(readOnly = true)
    public List<TransmissionDto> getTransmissions(String voyageId) {
        if (!voyageRepository.existsById(voyageId)) {
            throw new VoyageNotFoundException(voyageId);
        }
        return voyageMapper.toTransmissionDtos(transmissionRepository.findByVoyageIdOrderByIdAsc(voyageId));
    }
}
