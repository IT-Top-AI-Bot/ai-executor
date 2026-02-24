package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.service.ai.HomeworkAiService;
import com.aquadev.ittopaiexecutor.service.ai.VisionService;
import com.aquadev.ittopaiexecutor.service.extract.ImageExtractor;
import com.aquadev.ittopaiexecutor.service.extract.TextExtractor;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkSolverImpl implements HomeworkSolver {

    private final VisionService visionService;
    private final TextExtractor textExtractor;
    private final ImageExtractor imageExtractor;
    private final HomeworkAiService homeworkAiService;
    private final SubjectPromptService subjectPromptService;

    @Override
    public SolvedHomework solve(Path homeworkPath, Long specId) {
        TokenUsage tokenUsage = new TokenUsage();
        String text = textExtractor.extract(homeworkPath);

        Optional<SubjectPrompt> customPrompt = subjectPromptService.findBySpecId(specId);

        if (customPrompt.isPresent()) {
            log.info("Using custom prompts for specId={}", specId);
        } else {
            log.info("No custom prompts for specId={}, using defaults", specId);
        }

        String systemPrompt = customPrompt.map(SubjectPrompt::getSystemPrompt).orElse(null);
        String visionPrompt = customPrompt.map(SubjectPrompt::getVisionPrompt).orElse(null);

        try {
            Path tmpDir = Files.createTempDirectory("pdf-images");
            List<Path> images = imageExtractor.extractImages(homeworkPath, tmpDir);
            String vision = visionService.describeImages(images, tokenUsage, visionPrompt);

            String context = buildContext(text, vision);
            log.debug("Homework context:\n{}", context);

            SolvedHomework solution = homeworkAiService.solve(context, tokenUsage, systemPrompt);

            log.info("Token usage — {}", tokenUsage);

            return solution;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildContext(String text, String vision) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== ТЕКСТ ЗАДАНИЯ ===\n").append(text);

        if (vision != null && !vision.isBlank()) {
            sb.append("\n\n=== ВИЗУАЛЬНЫЕ МАТЕРИАЛЫ (примеры результата, схемы) ===\n").append(vision);
        }

        return sb.toString();
    }
}
