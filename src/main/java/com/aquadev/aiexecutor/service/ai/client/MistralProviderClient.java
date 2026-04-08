package com.aquadev.aiexecutor.service.ai.client;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.AiRequest;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class MistralProviderClient extends AbstractProviderClient {

    public MistralProviderClient(
            ChatClient mistralChatClient,
            TokenUsageHolder tokenUsageHolder
    ) {
        super(mistralChatClient, tokenUsageHolder);
    }

    @Override
    public AiProviderType providerType() {
        return AiProviderType.MISTRAL;
    }

    @Override
    @RateLimiter(name = "mistral-rps")
    public <T> T execute(AiRequest<T> request) {
        return doExecute(request);
    }
}
