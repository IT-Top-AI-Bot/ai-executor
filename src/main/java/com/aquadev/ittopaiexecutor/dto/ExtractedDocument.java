package com.aquadev.ittopaiexecutor.dto;

import java.util.Map;

public record ExtractedDocument(
        String text,
        Map<String, Object> metadata
) {
}
