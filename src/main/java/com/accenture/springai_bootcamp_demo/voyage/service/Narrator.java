package com.accenture.springai_bootcamp_demo.voyage.service;

import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

/**
 * Writes the official ship's log line for an hour, from what actually
 * happened in it. Free text on purpose: the line is for humans, Java never
 * reads it back.
 */
@Slf4j
@Component
class Narrator {

    private static final String INSTRUCTIONS = """
            You keep the official log of the generation ship Long Voyage.
            Everything that happened in hour %d:
            %s

            Write the log entry for this hour: at most two sentences and 40 words, past tense, calm and a little dramatic.
            Start with "Hour %d:". Mention only what is listed above; never invent events, people or numbers.
            """;

    private final ChatClient chatClient;

    Narrator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    Optional<String> narrate(int turn, List<String> happenings) {
        try {
            String entry = chatClient.prompt()
                    .user(INSTRUCTIONS.formatted(turn, String.join("\n", happenings), turn))
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
