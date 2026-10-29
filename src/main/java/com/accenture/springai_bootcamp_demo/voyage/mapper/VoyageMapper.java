package com.accenture.springai_bootcamp_demo.voyage.mapper;

import com.accenture.springai_bootcamp_demo.voyage.dto.CrewMemberDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.VoyageDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoyageMapper {

    VoyageDto toDto(Voyage voyage);
    CrewMemberDto toDto(CrewMember crewMember);
}
