package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;

public interface HomeworkAiService {

    SolvedHomework solve(String homeworkContext, TokenUsage tokenUsage, String systemPrompt);
}
