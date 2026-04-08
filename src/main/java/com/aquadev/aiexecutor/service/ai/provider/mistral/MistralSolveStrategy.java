package com.aquadev.aiexecutor.service.ai.provider.mistral;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import com.aquadev.aiexecutor.service.ai.mistral.AiResponseRecoveryService;
import com.aquadev.aiexecutor.service.ai.strategy.AiSolveStrategy;
import com.aquadev.aiexecutor.service.ai.strategy.AiVisionStrategy;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistralSolveStrategy implements AiSolveStrategy {

    private final ChatClient mistralChatClient;
    private final TokenUsageHolder tokenUsageHolder;
    private final AiVisionStrategy mistralVisionStrategy;
    private final AiResponseRecoveryService recoveryService;
    private final BeanOutputConverter<SolvedHomework> outputConverter = new BeanOutputConverter<>(SolvedHomework.class);

    @Value("${spring.ai.mistralai.chat.options.model:mistral-medium-latest}")
    private String model;

    @Value("${spring.ai.mistralai.chat.options.max-tokens:16384}")
    private int maxTokens;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.MISTRAL;
    }

    @Override
    @RateLimiter(name = "mistral-rps", fallbackMethod = "fallbackChat")
    public SolvedHomework executeSolveRequest(String systemPrompt, String userMessage, List<Media> images) {
        log.info("Solving with MISTRAL model={}", model);
        var options = MistralAiChatOptions.builder()
                .responseFormat(MistralAiApi.ChatCompletionRequest.ResponseFormat.jsonSchema(SolvedHomework.class))
                .maxTokens(maxTokens)
                .build();

        ChatResponse response = mistralChatClient.prompt()
                .system(systemPrompt)
                .user(buildUserMessage(userMessage, images))
                .options(options)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new AiResponseParsingException("Main AI model returned an empty response", null);
        }

        var usage = response.getMetadata().getUsage();
        tokenUsageHolder.get().addText(usage);

        String rawJson = response.getResult().getOutput().getText();
        log.debug("Main AI response ({} chars):\n{}", rawJson != null ? rawJson.length() : 0, rawJson);

        return parse(rawJson);
    }

    private String buildUserMessage(String userMessage, List<Media> images) {
        if (images == null || images.isEmpty()) return userMessage;
        return userMessage + "\n\n--- ATTACHED IMAGE DESCRIPTIONS ---\n"
                + mistralVisionStrategy.describeImages(images);
    }

    private SolvedHomework parse(String rawJson) {
        try {
            return outputConverter.convert(rawJson);
        } catch (Exception e) {
            return recoverOrThrow(rawJson, e);
        }
    }

    private SolvedHomework recoverOrThrow(String rawJson, Exception e) {
        log.warn("Failed to parse AI JSON response (chars={}), attempting recovery: {}",
                rawJson != null ? rawJson.length() : 0, e.getMessage());
        SolvedHomework recovered = recoveryService.tryRecover(rawJson);
        if (recovered != null) {
            log.warn("Recovered truncated AI response: filename={}, extension={}, contentLength={}",
                    recovered.filename(), recovered.extension(),
                    recovered.content() != null ? recovered.content().length() : 0);
            return recovered;
        }
        log.error("Failed to parse AI JSON response: {}", e.getMessage());
        throw new AiResponseParsingException("AI returned invalid JSON format", e);
    }

    public SolvedHomework fallbackChat(String systemPrompt, String userMessage, List<Media> images, Throwable throwable) {
        log.error("RateLimiter triggered or AI call failed: {}", throwable.getMessage());
        throw new AiResponseParsingException(
                "AI is temporarily unavailable or rate limit exceeded: " + throwable.getMessage(),
                throwable
        );
    }

}
