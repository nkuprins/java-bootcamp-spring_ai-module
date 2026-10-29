package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.repository.MemoryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Lets a crew member step back and draw conclusions from their recent
 * memories ("the hull is fixed", "the captain is hiding something"). The
 * conclusions are stored as memories too, which is how opinions change over
 * time instead of every turn starting from raw observations.
 */
@Slf4j
@Component
class Reflector {

    private static final int MAX_INSIGHTS = 2;

    private static final String INSTRUCTIONS = """
            You are thinking quietly to yourself aboard the generation ship Long Voyage.
            Your recent memories, oldest first:
            %s

            The ship right now: %s (each out of 100).

            What do you now believe, suspect or feel about the people, the ship or your situation?
            Give one or two short insights in the first person, each one sentence.
            Only conclusions that follow from these memories and the ship's state; nothing else.
            """;

    // Structured output target: Ollama is given this shape as a JSON schema and must answer in it
    record Reflection(List<String> insights) {
    }

    private final ChatClient chatClient;
    private final MemoryRepository memoryRepository;

    Reflector(ChatClient.Builder chatClientBuilder, MemoryRepository memoryRepository) {
        this.chatClient = chatClientBuilder.build();
        this.memoryRepository = memoryRepository;
    }

    List<String> reflect(CrewSituation situation) {
        List<String> memories = memoryRepository.findTop12ByCrewMemberIdOrderByIdDesc(situation.who().id())
                .reversed()
                .stream()
                .map(memory -> "- " + memory.getContent())
                .toList();
        if (memories.isEmpty()) {
            return List.of();
        }
        try {
            Reflection reflection = chatClient.prompt()
                    .system(situation.who().persona())
                    .user(INSTRUCTIONS.formatted(String.join("\n", memories), situation.ship().summary()))
                    .call()
                    .entity(Reflection.class, ChatClient.EntityParamSpec::useProviderStructuredOutput);
            if (reflection == null || reflection.insights() == null) {
                return List.of();
            }
            return reflection.insights().stream()
                    .filter(insight -> insight != null && !insight.isBlank())
                    .map(insight -> insight.replaceAll("\\s+", " ").strip())
                    .limit(MAX_INSIGHTS)
                    .toList();
        } catch (RuntimeException ex) {
            // A missed reflection only means this person keeps their old opinions a while longer
            log.warn("{} could not reflect on turn {}", situation.who().name(), situation.turn(), ex);
            return List.of();
        }
    }
}
