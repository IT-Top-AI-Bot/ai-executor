package com.aquadev.aiexecutor.service.ai.strategy;

import com.aquadev.aiexecutor.config.ai.AiProviderProperties;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderRouter {

    private final AiStrategyFactory strategyFactory;
    private final AiProviderProperties providerProperties;

    public SolvedHomework executeSolve(String systemPrompt, String userMessage, List<Media> images) {
        return tryChain(
                provider -> strategyFactory.getSolveStrategy(provider)
                        .executeSolveRequest(systemPrompt, userMessage, images)
        );
    }

    public String describeImages(List<Media> images) {
        return tryChain(
                provider -> strategyFactory.getVisionStrategy(provider)
                        .describeImages(images)
        );
    }

    private <T> T tryChain(ProviderCall<T> call) {
        List<AiProviderType> chain = providerProperties.orderedProviders();
        Exception lastException = null;

        for (AiProviderType provider : chain) {
            try {
                return call.execute(provider);
            } catch (RequestNotPermitted e) {
                log.warn("Provider {} rate limit exceeded, trying next...", provider);
                lastException = e;
            } catch (Exception e) {
                log.warn("Provider {} failed [{}]: {}", provider, e.getClass().getSimpleName(), e.getMessage(), e);
                lastException = e;
            }
        }

        throw new AiResponseParsingException(
                "All AI providers exhausted. Last: " + lastException.getMessage(),
                lastException
        );
    }

    @FunctionalInterface
    private interface ProviderCall<T> {
        T execute(AiProviderType provider) throws Exception;
    }
}
