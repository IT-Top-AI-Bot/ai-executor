package com.aquadev.aiexecutor.config.ai.mistral;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("ai.mistral.http")
public record MistralHttpProperties(
        Duration connectTimeout,
        Duration readTimeout,
        String proxyHost,
        int proxyPort
) {
    public MistralHttpProperties {
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(120);
        if (proxyHost == null) proxyHost = "";
    }
}
