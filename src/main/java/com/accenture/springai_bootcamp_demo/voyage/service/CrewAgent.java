package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.voyage.entity.Memory;
import com.accenture.springai_bootcamp_demo.voyage.entity.MemoryKind;
import com.accenture.springai_bootcamp_demo.voyage.service.MemoryRanking.ScoredMemory;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Plays one crew member for one turn: recalls what comes to mind, shows the
 * model the situation, and lets it pick an action through {@link CrewTools}.
 * Changes nothing itself; the caller applies the chosen action.
 */
@Slf4j
@Component
class CrewAgent {

    private static final int MEMORIES_IN_PROMPT = 5;
    private static final int WEIGHING_IN_PROMPT = 2;

    private static final String RULES = """
            You live aboard the generation ship Long Voyage, decades away from any planet.
            Every turn is one hour. Each turn you take exactly one action by calling one tool:
            moveTo, say, announce, repair or rest. Never describe a tool call in text: call it.
            Stay in character: act on what you know and feel. Keep anything you say to one or two sentences.
            Never repeat what you or others already said; react to what is new, or do something instead of talking.
            """;

    private static final String REMINDER = """
            You have not acted yet this hour. %s
            Call exactly one tool now: moveTo, say, announce, repair or rest.
            """;

    // Small models often answer in text or give up after a refused tool call, so they get one reminder
    private static final int ATTEMPTS = 2;

    private static final String SITUATION = """
            Turn %d. You are in %s. %s
            %s
            Ship status: %s (each out of 100).
            A hull below 50 leaks oxygen, power below 30 weakens the air scrubbers, and at oxygen 0 everyone dies.
            Hull and power are repaired in engineering; food and oxygen in the hydroponics bay.

            %s

            What do you do this hour? Call one tool.
            """;

    private final ChatClient chatClient;
    private final MemoryService memoryService;

    CrewAgent(ChatClient.Builder chatClientBuilder, MemoryService memoryService) {
        this.chatClient = chatClientBuilder.build();
        this.memoryService = memoryService;
    }

    Optional<CrewAction> decide(CrewSituation situation) {
        CrewTools tools = new CrewTools(situation);
        StringBuilder prompt = new StringBuilder(describe(situation));
        for (int attempt = 1; attempt <= ATTEMPTS && tools.chosenAction().isEmpty(); attempt++) {
            if (attempt > 1) {
                prompt.append('\n')
                        .append(REMINDER.formatted(tools.lastRefusal().map(r -> "Last time: " + r).orElse("")));
            }
            ask(situation, prompt.toString(), tools);
        }
        return tools.chosenAction();
    }

    private void ask(CrewSituation situation, String prompt, CrewTools tools) {
        try {
            String reply = chatClient.prompt()
                    .system(situation.who().persona() + "\n\n" + RULES)
                    .user(prompt)
                    .tools(tools)
                    .call()
                    .content();
            log.debug("{} on turn {}: {}", situation.who().name(), situation.turn(), reply);
            if (tools.chosenAction().isEmpty() && reply != null) {
                tools.followWrittenCall(reply).ifPresent(result ->
                        log.debug("{} wrote a tool call as text, ran it: {}", situation.who().name(), result));
            }
        } catch (RuntimeException ex) {
            // One confused crew member should not stop the whole turn; they just lose their action
            log.warn("{} could not decide on turn {}", situation.who().name(), situation.turn(), ex);
        }
    }

    private String describe(CrewSituation situation) {
        String company = situation.isAlone()
                ? "You are alone."
                : "With you: " + String.join(", ", situation.companions()) + ".";
        String lastWords = situation.lastWords() == null
                ? ""
                : "The last thing you said was: \"" + situation.lastWords() + "\"";
        return SITUATION.formatted(situation.turn(), situation.location().label(), company, lastWords,
                situation.ship().summary(), memories(situation, company));
    }

    private String memories(CrewSituation situation, String company) {
        List<Memory> weighing = memoryService.weighingOnMind(situation.who().id(), situation.turn(), WEIGHING_IN_PROMPT);
        Set<Long> alreadyShown = weighing.stream().map(Memory::getId).collect(Collectors.toSet());

        // Their own words are left out: the prompt shows their last words, and older ones only invite parroting
        String now = "In %s. %s Ship: %s.".formatted(situation.location().label(), company, situation.ship().summary());
        List<Memory> recalled = memoryService.recall(situation.who().id(), now, situation.turn(), MEMORIES_IN_PROMPT * 2)
                .stream()
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

    private static String bullets(List<Memory> memories) {
        return memories.stream().map(memory -> "- " + memory.getContent()).collect(Collectors.joining("\n"));
    }
}
