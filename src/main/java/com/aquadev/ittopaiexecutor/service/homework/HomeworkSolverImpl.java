package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.service.ai.HomeworkAiService;
import com.aquadev.ittopaiexecutor.service.document.DocumentStrategyResolver;
import com.aquadev.ittopaiexecutor.service.document.extractor.DocumentExtractor;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkSolverImpl implements HomeworkSolver {

    private final ChatClient chatClient;
    private final DocumentStrategyResolver strategyResolver;
    private final HomeworkAiService homeworkAiService;
    private final SubjectPromptService subjectPromptService;

    @Override
    public SolvedHomework solve(Path homeworkPath, Long specId) {
        TokenUsage tokenUsage = new TokenUsage();

        String filename = homeworkPath.getFileName().toString();
        byte[] bytes = readBytes(homeworkPath);

        DocumentExtractor extractor = strategyResolver.resolve(filename);
        ExtractedDocument extracted = extractor.extract(bytes, filename, chatClient);
        log.debug("Extracted document text length={}", extracted.text().length());

        Optional<SubjectPrompt> customPrompt = subjectPromptService.findBySpecId(specId);
        if (customPrompt.isPresent()) {
            log.info("Using custom prompt for specId={}", specId);
        } else {
            log.info("No custom prompt for specId={}, using default", specId);
        }
        String systemPrompt = customPrompt.map(SubjectPrompt::getSystemPrompt).orElse(null);

        SolvedHomework solution = homeworkAiService.solve(extracted.text(), tokenUsage, systemPrompt);
        log.info("Token usage — {}", tokenUsage);

        return solution;
    }

    private byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read homework file: " + path, e);
        }
    }
}
