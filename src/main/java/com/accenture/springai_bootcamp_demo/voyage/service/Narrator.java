package com.accenture.springai_bootcamp_demo.voyage.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Writes the official ship's log line for an hour, from what actually
 * happened in it. Free text on purpose: the line is for humans, Java never
 * reads it back.
 */
@Slf4j
@Component
class Narrator {

    private static final PromptTemplate INSTRUCTIONS = new PromptTemplate(new ClassPathResource("prompts/narration.st"));

    private final ChatClient chatClient;

    Narrator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    Optional<String> narrate(int turn, List<String> happenings) {
        try {
            String entry = chatClient.prompt()
                    .user(INSTRUCTIONS.render(Map.of("turn", turn, "happenings", String.join("\n", happenings))))
                    .call()
                    .content();
            // One log line: models sometimes break their answer over several lines
            return Optional.ofNullable(entry).map(text -> text.replaceAll("\\s+", " ").strip()).filter(text -> !text.isEmpty());
        } catch (RuntimeException ex) {
            // The log line is decoration; the turn already happened without it
            log.warn("No narration for turn {}", turn, ex);
            return Optional.empty();
        }
    }
}
