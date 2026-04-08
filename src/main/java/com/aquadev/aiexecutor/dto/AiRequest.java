package com.aquadev.aiexecutor.dto;

import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;

import java.util.List;

public record AiRequest<T>(
        String systemPrompt,
        String userMessage,
        List<Media> images,
        ChatOptions options,
        Class<T> clazz
) {
    public AiRequest {
        images = images != null ? List.copyOf(images) : List.of();
    }
}
