package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

import java.nio.file.Path;

public interface HomeworkSolver {

    SolvedHomework solve(Path homeworkPath, Long specId);
}
