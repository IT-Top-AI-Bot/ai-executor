package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.SolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface HomeworkSolver {

    SolvedHomework solve(SolveRequest request);
}
