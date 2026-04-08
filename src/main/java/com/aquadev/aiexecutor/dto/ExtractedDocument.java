package com.aquadev.aiexecutor.dto;

import org.springframework.ai.content.Media;

import java.util.List;
import java.util.Map;

public record ExtractedDocument(
        String text,
        Map<String, Object> metadata,
        List<Media> images
) {
    public ExtractedDocument(String text, Map<String, Object> metadata) {
        this(text, metadata, List.of());
    }
}
