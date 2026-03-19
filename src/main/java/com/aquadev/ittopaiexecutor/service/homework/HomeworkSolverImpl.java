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
    public SolvedHomework solve(byte[] content, String filename, Long specId,
                                String theme, String teacherFio, String nameSpec, String comment) {
        Optional<SubjectPrompt> customPrompt = subjectPromptService.findBySpecId(specId);

        if (customPrompt.isPresent() && customPrompt.get().getStaticText() != null) {
            log.info("Using static text for specId={}, skipping AI", specId);
            return new SolvedHomework(null, null, customPrompt.get().getStaticText());
        }

        TokenUsage tokenUsage = new TokenUsage();

        DocumentExtractor extractor = strategyResolver.resolve(filename);
        ExtractedDocument extracted = extractor.extract(content, filename, chatClient);
        log.debug("Extracted document: filename={}, textLength={}", filename, extracted.text().length());


        if (customPrompt.isPresent()) {
            log.info("Using custom AI prompt for specId={}", specId);
        } else {
            log.info("No custom prompt for specId={}, using default", specId);
        }
        String systemPrompt = customPrompt.map(SubjectPrompt::getSystemPrompt).orElse(null);

        SolvedHomework solution = homeworkAiService.solve(
                extracted.text(), tokenUsage, systemPrompt,
                theme, teacherFio, nameSpec, comment,
                extracted.images());
        log.info("Token usage — {}", tokenUsage);

        return solution;
    }
}
