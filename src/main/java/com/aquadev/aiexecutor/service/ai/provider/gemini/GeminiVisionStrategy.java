package com.aquadev.aiexecutor.service.ai.provider.gemini;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import com.aquadev.aiexecutor.service.ai.strategy.AiVisionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiVisionStrategy implements AiVisionStrategy {

    private final ChatClient geminiChatClient;
    private final TokenUsageHolder tokenUsageHolder;

    @Value("${spring.ai.google.genai.vision.model:gemini-2.0-flash}")
    private String visionModel;

    @Value("${spring.ai.google.genai.vision.temperature:0.2}")
    private double visionTemperature;

    @Value("classpath:prompts/vision-system-prompt.md")
    private Resource visionSystemPromptResource;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.GEMINI;
    }

    @Override
    public String describeImages(List<Media> images) {
        if (images == null || images.isEmpty()) return "";

        var options = GoogleGenAiChatOptions.builder()
                .model(visionModel)
                .temperature(visionTemperature)
                .build();

        var response = geminiChatClient.prompt()
                .system(visionSystemPromptResource)
                .user(u -> {
                    u.text("Please describe the provided images in detail.");
                    images.forEach(u::media);
                })
                .options(options)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new AiResponseParsingException("Vision AI model returned an empty response");
        }

        tokenUsageHolder.get().addText(response.getMetadata().getUsage());

        return response.getResult().getOutput().getText();
    }
}
