package com.aquadev.ittopaiexecutor.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record SolvedHomework(

        @JsonPropertyDescription(
                "Snake_case filename in English, 2–5 words describing the topic. " +
                        "Good: relational_databases, html_layout_forms, python_file_io. " +
                        "Bad: task_1, homework, rabota_1, prakticheskaya_rabota")
        String filename,

        @JsonPropertyDescription(
                "File extension without dot. " +
                        "html — web layouts, CSS/JS with visual styling; " +
                        "py — Python code; " +
                        "js — JavaScript without HTML wrapper; " +
                        "docx — text, essays, theory, answers; " +
                        "txt — plain calculations or tables; " +
                        "or the extension of any other programming language")
        String extension,

        @JsonPropertyDescription(
                "Complete ready-to-save file content. " +
                        "No preamble, work descriptions, or meta-comments about the task.")
        String content
) {
}
