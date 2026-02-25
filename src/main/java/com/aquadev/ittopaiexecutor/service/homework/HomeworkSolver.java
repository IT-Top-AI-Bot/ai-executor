package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface HomeworkSolver {

    /**
     * Solves the homework given as raw bytes.
     *
     * @param content  raw bytes of the homework file
     * @param filename original filename with extension (e.g. "task.pdf")
     * @param specId   subject specification ID — used to look up a custom prompt
     */
    SolvedHomework solve(byte[] content, String filename, Long specId);
}
