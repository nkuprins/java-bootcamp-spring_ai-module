package com.accenture.springai_bootcamp_demo.voyage.service;

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Plays one crew member for one turn: recalls what comes to mind, shows the
 * model the situation, and lets it pick an action through {@link CrewTools}.
 * Changes nothing itself; the caller applies the chosen action.
 */
@Slf4j
@Component
class CrewAgent {

    private static final String RULES = new PromptTemplate(new ClassPathResource("prompts/crew-rules.st")).render();
    private static final PromptTemplate REMINDER = new PromptTemplate(new ClassPathResource("prompts/crew-reminder.st"));

    // Small models often answer in text or give up after a refused tool call, so they get one reminder
    private static final int ATTEMPTS = 2;

    private static final PromptTemplate SITUATION =
            new PromptTemplate(new ClassPathResource("prompts/crew-situation.st"));

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
                        .append(REMINDER.render(Map.of(
                                "lastRefusal", tools.lastRefusal().map(r -> "Last time: " + r).orElse(""))));
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
        String lastWords = situation.lastWords() == null
                ? ""
                : "The last thing you said was: \"" + situation.lastWords() + "\"";
        String now = "In %s. %s Ship: %s.".formatted(
                situation.location().label(), situation.company(), situation.ship().summary());
        return SITUATION.render(Map.of(
                "turn", situation.turn(),
                "room", situation.location().label(),
                "company", situation.company(),
                "lastWords", lastWords,
                "ship", situation.ship().summary(),
                "mind", memoryService.describeMind(situation.who().id(), now, situation.turn())));
    }
}
