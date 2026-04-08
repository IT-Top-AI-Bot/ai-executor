package com.aquadev.aiexecutor.service.ai.homework;

import com.aquadev.aiexecutor.dto.AiSolveRequest;
import com.aquadev.aiexecutor.dto.SolvedHomework;

public interface HomeworkAiService {

    SolvedHomework solve(AiSolveRequest request);
}
