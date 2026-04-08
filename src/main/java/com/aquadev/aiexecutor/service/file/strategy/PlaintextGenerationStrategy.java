package com.aquadev.aiexecutor.service.file.strategy;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class PlaintextGenerationStrategy implements FileGenerationStrategy {

    @Override
    public Set<String> supportedExtensions() {
        return Set.of(); // пустой Set = fallback
    }

    @Override
    public byte[] generate(String content) {
        return content.getBytes(StandardCharsets.UTF_8);
    }
}
