package com.aquadev.ittopaiexecutor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubjectPromptRequest(
        @NotNull Long specId,
        @NotBlank String nameSpec,
        @NotBlank String systemPrompt,
        String visionPrompt
) {
}
