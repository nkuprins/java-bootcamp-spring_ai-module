package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.config.VoyageProperties;
import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.mapper.VoyageMapper;
import com.accenture.springai_bootcamp_demo.voyage.repository.LogEntryRepository;
import com.accenture.springai_bootcamp_demo.voyage.repository.VoyageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoyageService {
    private final VoyageRepository voyageRepository;
    private final LogEntryRepository logEntryRepository;
    private final VoyageProperties voyageProperties;
    private final VoyageMapper voyageMapper;

    @Transactional
    public VoyageDto createVoyage() {
        Voyage voyage = Voyage.create();
        voyageProperties.crew().forEach(profile -> voyage.addCrewMember(
                CrewMember.of(profile.name(), profile.role(), profile.persona(), profile.location())));
        voyageRepository.save(voyage);
        log.info("Launched voyage {} with {} crew", voyage.getId(), voyage.getCrew().size());
        return voyageMapper.toDto(voyage);
    }

    @Transactional(readOnly = true)
    public VoyageDto getVoyage(String voyageId) {
        return voyageMapper.toDto(voyageRepository.findWithCrewById(voyageId)
                .orElseThrow(() -> new VoyageNotFoundException(voyageId)));
    }

    @Transactional(readOnly = true)
    public List<LogEntryDto> getLog(String voyageId) {
        if (!voyageRepository.existsById(voyageId)) {
            throw new VoyageNotFoundException(voyageId);
        }
        return voyageMapper.toLogDtos(logEntryRepository.findByVoyageIdOrderByIdAsc(voyageId));
    }
}
