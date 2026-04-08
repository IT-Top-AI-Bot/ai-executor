package com.aquadev.aiexecutor.dto;

public record SubjectPromptResponse(
        Long specId,
        String nameSpec,
        String systemPrompt,
        String staticText
) {
}
