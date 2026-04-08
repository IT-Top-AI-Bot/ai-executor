package com.aquadev.aiexecutor.service.ai.client;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.AiRequest;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GeminiProviderClient extends AbstractProviderClient {

    public GeminiProviderClient(
            ChatClient geminiChatClient,
            TokenUsageHolder tokenUsageHolder
    ) {
        super(geminiChatClient, tokenUsageHolder);
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.GEMINI;
    }

    @Override
    @RateLimiter(name = "gemini-rps")
    public <T> T execute(AiRequest<T> request) {
        return doExecute(request);
    }
}
