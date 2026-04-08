package com.aquadev.aiexecutor.config.ai.gemini;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("ai.gemini.http")
public record GeminiHttpProperties(
        Duration connectTimeout,
        Duration readTimeout,
        String proxyHost,
        int proxyPort
) {
    public GeminiHttpProperties {
        if (connectTimeout == null) connectTimeout = Duration.ofSeconds(10);
        if (readTimeout == null) readTimeout = Duration.ofSeconds(120);
        if (proxyHost == null) proxyHost = "";
    }
}
