package com.aquadev.aiexecutor.dto;

import org.springframework.ai.content.Media;

import java.util.List;

public record AiSolveRequest(
        String homeworkContext,
        String systemPrompt,
        String theme,
        String teacherFio,
        String nameSpec,
        String comment,
        List<Media> images
) {
}
