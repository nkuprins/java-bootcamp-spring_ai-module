package com.accenture.springai_bootcamp_demo.voyage.service;

import com.accenture.springai_bootcamp_demo.client.ModelServerException;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Answers Mission Control's question as one crew member, from what that person
 * knows. The question doubles as the memory query, so asking about something
 * brings up what they remember about it.
 */
@Component
class Interviewer {

    private static final String RULES = new PromptTemplate(new ClassPathResource("prompts/interview-rules.st")).render();
    private static final PromptTemplate SITUATION =
            new PromptTemplate(new ClassPathResource("prompts/interview-situation.st"));

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
                    .user(SITUATION.render(Map.of(
                            "turn", situation.turn(),
                            "room", situation.location().label(),
                            "company", situation.company(),
                            "ship", situation.ship().summary(),
                            "mind", mind,
                            "question", question)))
                    .call()
                    .content();
        } catch (RuntimeException ex) {
            throw new ModelServerException("The model could not answer as " + situation.who().name(), ex);
        }
        if (!StringUtils.hasText(answer)) {
            throw new ModelServerException(situation.who().name() + " gave no answer");
        }
        return answer.replaceAll("\\s+", " ").strip();
    }
}
