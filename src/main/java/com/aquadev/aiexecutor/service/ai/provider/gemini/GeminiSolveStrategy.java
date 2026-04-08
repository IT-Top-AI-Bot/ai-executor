package com.aquadev.aiexecutor.service.ai.provider.gemini;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import com.aquadev.aiexecutor.service.ai.strategy.AiSolveStrategy;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiSolveStrategy implements AiSolveStrategy {

    private final ChatClient geminiChatClient;
    private final TokenUsageHolder tokenUsageHolder;
    private final BeanOutputConverter<SolvedHomework> outputConverter = new BeanOutputConverter<>(SolvedHomework.class);

    @Value("${spring.ai.google.genai.chat.options.model:gemini-2.0-flash}")
    private String model;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.GEMINI;
    }

    @Override
    @RateLimiter(name = "gemini-rps", fallbackMethod = "fallback")
    public SolvedHomework executeSolveRequest(String systemPrompt, String userMessage, List<Media> images) {
        var options = GoogleGenAiChatOptions.builder()
                .model(model)
                .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
                .responseSchema(outputConverter.getJsonSchema())
                .build();

        log.info("Solving with GEMINI model={}", model);
        ChatResponse response = geminiChatClient.prompt()
                .system(systemPrompt)
                .user(u -> {
                    u.text(userMessage);
                    if (images != null) images.forEach(u::media);
                })
                .options(options)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new AiResponseParsingException("Gemini returned empty response", null);
        }

        tokenUsageHolder.get().addText(response.getMetadata().getUsage());
        String rawJson = response.getResult().getOutput().getText();
        log.debug("Gemini response ({} chars)", rawJson != null ? rawJson.length() : 0);

        try {
            return outputConverter.convert(rawJson);
        } catch (Exception e) {
            throw new AiResponseParsingException("Gemini returned invalid JSON", e);
        }
    }

    public SolvedHomework fallback(String sp, String um, List<Media> i, Throwable t) {
        throw new AiResponseParsingException("Gemini unavailable: " + t.getMessage(), t);
    }
}
