package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.AiSolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface HomeworkAiService {

    SolvedHomework solve(AiSolveRequest request);
}
