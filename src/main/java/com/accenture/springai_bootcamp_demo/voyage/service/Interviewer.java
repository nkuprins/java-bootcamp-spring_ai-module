package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.client.OpenRouterException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Answers Mission Control's question as one crew member, from what that person
 * knows. The question doubles as the memory query, so asking about something
 * brings up what they remember about it.
 */
@Component
class Interviewer {

    private static final String RULES = """
            Mission Control on Earth is speaking with you over a private channel; nobody else on board can hear.
            Answer in character, in one to three sentences, only from what you know and remember.
            You may evade, refuse or lie if your character would.
            """;

    private static final String SITUATION = """
            Turn %d. You are in %s. %s
            Ship status: %s (each out of 100).

            %s

            Mission Control asks: "%s"
            """;

    private final ChatClient chatClient;
    private final MemoryService memoryService;

    Interviewer(ChatClient.Builder chatClientBuilder, MemoryService memoryService) {
        this.chatClient = chatClientBuilder.build();
        this.memoryService = memoryService;
    }

    // Unlike a turn, Mission Control is waiting for this answer, so a failed call is reported instead of skipped
    String answer(CrewSituation situation, String question) {
        String mind = memoryService.describeMind(situation.who().id(), question, situation.turn());
        String answer;
        try {
            answer = chatClient.prompt()
                    .system(situation.who().persona() + "\n\n" + RULES)
                    .user(SITUATION.formatted(situation.turn(), situation.location().label(), situation.company(),
                            situation.ship().summary(), mind, question))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            throw new OpenRouterException("The model could not answer as " + situation.who().name(), ex);
        }
        if (!StringUtils.hasText(answer)) {
            throw new OpenRouterException(situation.who().name() + " gave no answer");
        }
        return answer.replaceAll("\\s+", " ").strip();
    }
}
