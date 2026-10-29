package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.InterviewDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;
import com.accenture.springai_bootcamp_demo.voyage.entity.Voyage;
import com.accenture.springai_bootcamp_demo.voyage.repository.VoyageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mission Control talking privately to one crew member. The conversation is
 * remembered by them, so a question can change what they do next. Not behind
 * the busy guard: it never touches the ship.
 */
@Service
@RequiredArgsConstructor
public class InterviewService {
    private final VoyageRepository voyageRepository;
    private final Interviewer interviewer;
    private final MemoryService memoryService;
    private final TransactionTemplate transactionTemplate;

    public InterviewDto interview(String voyageId, Long crewMemberId, String question) {
        CrewSituation situation = transactionTemplate.execute(status -> {
            Voyage voyage = voyageRepository.findWithCrewById(voyageId)
                    .orElseThrow(() -> new VoyageNotFoundException(voyageId));
            if (voyage.isLost()) {
                throw new VoyageStateException("Voyage " + voyageId + " is lost; nobody is left to answer");
            }
            return voyage.getCrew().stream()
                    .filter(member -> member.getId().equals(crewMemberId))
                    .findFirst()
                    .map(member -> CrewSituation.of(voyage, member))
                    .orElseThrow(() -> new CrewMemberNotFoundException(voyageId, crewMemberId));
        });

        String asked = question.strip();
        String answer = interviewer.answer(situation, asked);
        memoryService.recordAll(List.of(new MemoryDraft(crewMemberId, situation.turn(), MemoryKind.INTERVIEW,
                "Mission Control asked me privately: \"%s\" I answered: \"%s\"".formatted(asked, answer))));
        return new InterviewDto(crewMemberId, situation.who().name(), situation.turn(), asked, answer);
    }
}
