package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.repository.MemoryRepository;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
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

    // Small models sometimes answer with broken JSON or no insights; told what went wrong, they usually manage
    private static final int ATTEMPTS = 2;

    private static final PromptTemplate INSTRUCTIONS = new PromptTemplate(new ClassPathResource("prompts/reflection.st"));
    private static final PromptTemplate RETRY = new PromptTemplate(new ClassPathResource("prompts/reflection-retry.st"));

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
        String instructions = INSTRUCTIONS.render(Map.of(
                "memories", String.join("\n", memories),
                "ship", situation.ship().summary()));
        String prompt = instructions;
        for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
            String problem;
            try {
                List<String> insights = insightsOf(chatClient.prompt()
                        .system(situation.who().persona())
                        .user(prompt)
                        .call()
                        .entity(Reflection.class, ChatClient.EntityParamSpec::useProviderStructuredOutput));
                if (!insights.isEmpty()) {
                    return insights;
                }
                problem = "it contained no insights";
            } catch (RuntimeException ex) {
                log.warn("{} could not reflect on turn {} (attempt {})", situation.who().name(), situation.turn(),
                        attempt, ex);
                problem = "it could not be read as JSON";
            }
            prompt = instructions + "\n" + RETRY.render(Map.of("problem", problem));
        }
        // A missed reflection only means this person keeps their old opinions a while longer
        return List.of();
    }

    private static List<String> insightsOf(Reflection reflection) {
        if (reflection == null || reflection.insights() == null) {
            return List.of();
        }
        return reflection.insights().stream()
                .filter(insight -> insight != null && !insight.isBlank())
                .map(insight -> insight.replaceAll("\\s+", " ").strip())
                .limit(MAX_INSIGHTS)
                .toList();
    }
}
