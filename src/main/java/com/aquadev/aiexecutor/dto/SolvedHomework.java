package com.aquadev.aiexecutor.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SolvedHomework(

        @JsonPropertyDescription(
                "Short lowercase filename, 1–2 words max, like a student would name it casually. " +
                        "Good: dz, homework, task1, dz_1, hw, practice, lab2, dz2. " +
                        "Bad: relational_databases_lab_work, html_layout_forms_practice")
        String filename,

        @JsonPropertyDescription(
                "File extension without dot. " +
                        "Use a code extension ONLY when the entire content is a single, complete, runnable file — no markdown, no prose explanations, no task headers. " +
                        "html — a single web page with CSS/JS; " +
                        "py — a single Python script; " +
                        "js — a single JavaScript script; " +
                        "or any other single-language source file. " +
                        "Use docx when the answer contains multiple tasks, explanations, mixed text and code, headers, or any prose. " +
                        "Use txt for plain calculations or tables without formatting.")
        String extension,

        @JsonPropertyDescription(
                "Complete ready-to-save file content. " +
                        "No preamble, work descriptions, or meta-comments about the task.")
        String content
) {
}
