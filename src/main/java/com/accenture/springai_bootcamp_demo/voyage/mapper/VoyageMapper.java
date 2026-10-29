package com.accenture.springai_bootcamp_demo.voyage.mapper;

import com.accenture.springai_bootcamp_demo.voyage.dto.CrewMemberDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.LogEntryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryScoreDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.LogEntry;
import com.accenture.springai_bootcamp_demo.voyage.entity.Memory;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VoyageMapper {

    VoyageDto toDto(Voyage voyage);
    CrewMemberDto toDto(CrewMember crewMember);
    LogEntryDto toDto(LogEntry logEntry);
    List<LogEntryDto> toLogDtos(List<LogEntry> logEntries);

    @Mapping(target = "score", ignore = true)
    MemoryDto toDto(Memory memory);
    MemoryDto toDto(Memory memory, MemoryScoreDto score);
    List<MemoryDto> toMemoryDtos(List<Memory> memories);
}
