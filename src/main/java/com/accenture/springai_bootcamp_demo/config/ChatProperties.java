package com.accenture.springai_bootcamp_demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chat")
public record ChatProperties(String systemPrompt) {
    public ChatProperties {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a helpful, concise assistant.";
        }
    }
}
