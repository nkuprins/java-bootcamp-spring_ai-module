package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.config.ChatProperties;
import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Thin client over the chat model (Ollama), backed by Spring AI's
 * {@link ChatClient}. Keeps the public surface intentionally small: callers
 * hand over the conversation history and receive the assistant's reply text.
 */
@Slf4j
@Component
public class ChatModelClient {
    private final ChatClient chatClient;

    public ChatModelClient(ChatClient.Builder chatClientBuilder, ChatProperties properties) {
        this.chatClient = chatClientBuilder
                .defaultSystem(properties.systemPrompt())
                .build();
    }

    public String complete(List<ChatMessage> history) {
        String reply = call(history);

        return extractContent(reply);
    }

    private String call(List<ChatMessage> history) {
        try {
            return chatClient.prompt()
                    .messages(history.stream().map(ChatModelClient::toPromptMessage).toList())
                    .call()
                    .content();
        } catch (ModelServerException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Chat model request failed", ex);
            throw new ModelServerException("Failed to reach the model server: " + ex.getMessage(), ex);
        }
    }

    private static Message toPromptMessage(ChatMessage chatMessage) {
        return switch (chatMessage.getRole()) {
            case SYSTEM -> new SystemMessage(chatMessage.getContent());
            case USER -> new UserMessage(chatMessage.getContent());
            case ASSISTANT -> new AssistantMessage(chatMessage.getContent());
        };
    }

    private String extractContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ModelServerException("The model returned an empty response");
        }
        return content.trim();
    }
}
