package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface HomeworkSolver {

    SolvedHomework solve(byte[] content, String filename, Long specId,
                         String theme, String teacherFio, String nameSpec, String comment);
}
