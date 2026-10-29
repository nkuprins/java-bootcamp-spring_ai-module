package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryDto;
import com.accenture.springai_bootcamp_demo.voyage.dto.MemoryScoreDto;
import com.accenture.springai_bootcamp_demo.voyage.entity.CrewMember;
import com.accenture.springai_bootcamp_demo.voyage.entity.Memory;
import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;
import com.accenture.springai_bootcamp_demo.voyage.mapper.VoyageMapper;
import com.accenture.springai_bootcamp_demo.voyage.repository.CrewMemberRepository;
import com.accenture.springai_bootcamp_demo.voyage.repository.MemoryRepository;
import com.accenture.springai_bootcamp_demo.voyage.service.MemoryRanking.ScoredMemory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class MemoryService {

    // nomic-embed-text is trained with these task prefixes and ranks better when they are used
    private static final String DOCUMENT_PREFIX = "search_document: ";
    private static final String QUERY_PREFIX = "search_query: ";

    // Reflections, things seen first-hand and Earth messages; they fade from the mind after half a day
    private static final int WEIGHTY_IMPORTANCE = 7;
    private static final int WEIGHTY_FOR_TURNS = 12;
    private static final int WEIGHING_IN_PROMPT = 2;
    private static final int MEMORIES_IN_PROMPT = 5;

    private final MemoryRepository memoryRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final EmbeddingModel embeddingModel;
    private final TransactionTemplate transactionTemplate;
    private final VoyageMapper voyageMapper;

    void recordAll(List<MemoryDraft> drafts) {
        if (drafts.isEmpty()) {
            return;
        }
        List<float[]> embeddings = embeddingModel.embed(
                drafts.stream().map(draft -> DOCUMENT_PREFIX + draft.content()).toList());

        transactionTemplate.executeWithoutResult(status -> {
            List<Memory> memories = new ArrayList<>();
            for (int i = 0; i < drafts.size(); i++) {
                MemoryDraft draft = drafts.get(i);
                CrewMember crewMember = crewMemberRepository.getReferenceById(draft.crewMemberId());
                memories.add(Memory.of(crewMember, draft.turn(), draft.kind(), draft.content(), embeddings.get(i)));
            }
            memoryRepository.saveAll(memories);
        });
    }

    /**
     * What a crew member has in mind in a situation, as prompt text: their weightiest recent memories,
     * then the ones that fit the situation. Their own words are left out of the second part, because
     * replaying them only invites parroting.
     */
    String describeMind(Long crewMemberId, String situation, int currentTurn) {
        List<Memory> weighing = weighingOnMind(crewMemberId, currentTurn);
        Set<Long> alreadyShown = weighing.stream().map(Memory::getId).collect(Collectors.toSet());
        List<Memory> recalled = recall(crewMemberId, situation, currentTurn, MEMORIES_IN_PROMPT * 2).stream()
                .map(ScoredMemory::memory)
                .filter(memory -> memory.getKind() != MemoryKind.SPOKE && !alreadyShown.contains(memory.getId()))
                .limit(MEMORIES_IN_PROMPT)
                .toList();

        StringBuilder text = new StringBuilder();
        if (!weighing.isEmpty()) {
            text.append("Weighing on your mind:\n").append(bullets(weighing)).append("\n\n");
        }
        text.append("What comes to mind:\n").append(recalled.isEmpty() ? "Nothing in particular." : bullets(recalled));
        return text.toString();
    }

    // Big news stays on someone's mind wherever they are, however little it has to do with the room or the ship
    private List<Memory> weighingOnMind(Long crewMemberId, int currentTurn) {
        return memoryRepository.findByCrewMemberId(crewMemberId).stream()
                .filter(memory -> memory.getKind().importance() >= WEIGHTY_IMPORTANCE)
                .filter(memory -> currentTurn - memory.getTurn() <= WEIGHTY_FOR_TURNS)
                .sorted(Comparator.comparingInt((Memory memory) -> memory.getKind().importance())
                        .thenComparingInt(Memory::getTurn)
                        .reversed())
                .limit(WEIGHING_IN_PROMPT)
                .toList();
    }

    private static String bullets(List<Memory> memories) {
        return memories.stream().map(memory -> "- " + memory.getContent()).collect(Collectors.joining("\n"));
    }

    List<ScoredMemory> recall(Long crewMemberId, String situation, int currentTurn, int limit) {
        float[] query = embeddingModel.embed(QUERY_PREFIX + situation);
        return MemoryRanking.rank(memoryRepository.findByCrewMemberId(crewMemberId), query, currentTurn, limit);
    }

    public List<MemoryDto> getMemories(String voyageId, Long crewMemberId, String query, int limit) {
        CrewMember crewMember = crewMemberRepository.findWithVoyageByIdAndVoyageId(crewMemberId, voyageId)
                .orElseThrow(() -> new CrewMemberNotFoundException(voyageId, crewMemberId));
        if (query == null || query.isBlank()) {
            return voyageMapper.toMemoryDtos(memoryRepository.findByCrewMemberIdOrderByIdDesc(crewMemberId));
        }
        return recall(crewMemberId, query, crewMember.getVoyage().getTurn(), limit).stream()
                .map(scored -> voyageMapper.toDto(scored.memory(), new MemoryScoreDto(
                        scored.relevance(), scored.recency(), scored.importance(), scored.total())))
                .toList();
    }
}
