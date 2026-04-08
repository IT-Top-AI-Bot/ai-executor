package com.aquadev.aiexecutor.service.homework;

import com.aquadev.aiexecutor.dto.SolveRequest;
import com.aquadev.aiexecutor.dto.SolvedHomework;

public interface HomeworkSolver {

    SolvedHomework solve(SolveRequest request);
}
