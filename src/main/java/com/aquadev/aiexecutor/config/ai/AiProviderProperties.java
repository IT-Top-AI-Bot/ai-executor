package com.aquadev.aiexecutor.config.ai;

import com.aquadev.aiexecutor.dto.AiProviderType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("ai")
public record AiProviderProperties(
        AiProviderType defaultProvider,
        List<AiProviderType> fallbackProviders
) {
    public AiProviderProperties {
        if (defaultProvider == null) defaultProvider = AiProviderType.MISTRAL;
        if (fallbackProviders == null) fallbackProviders = List.of();
    }

    public List<AiProviderType> orderedProviders() {
        List<AiProviderType> chain = new ArrayList<>();
        chain.add(defaultProvider);
        chain.addAll(fallbackProviders);
        return chain;
    }
}
