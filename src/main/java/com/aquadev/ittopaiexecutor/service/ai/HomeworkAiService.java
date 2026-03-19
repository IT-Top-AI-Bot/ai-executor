package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import org.springframework.ai.content.Media;

import java.util.List;

public interface HomeworkAiService {

    SolvedHomework solve(
            String homeworkContext,
            TokenUsage tokenUsage,
            String systemPrompt,
            String theme,
            String teacherFio,
            String nameSpec,
            String comment,
            List<Media> images
    );
}
