package com.aquadev.ittopaiexecutor.dto;

public record SubjectPromptResponse(
        Long specId,
        String nameSpec,
        String systemPrompt,
        String visionPrompt,
        String staticText
) {
}
