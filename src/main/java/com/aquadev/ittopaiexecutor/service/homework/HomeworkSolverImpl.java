package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.aop.TrackTokenUsage;
import com.aquadev.ittopaiexecutor.dto.AiSolveRequest;
import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.dto.SolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.service.ai.HomeworkAiService;
import com.aquadev.ittopaiexecutor.service.document.DocumentStrategyResolver;
import com.aquadev.ittopaiexecutor.service.document.extractor.DocumentExtractor;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkSolverImpl implements HomeworkSolver {

    private final DocumentStrategyResolver strategyResolver;
    private final HomeworkAiService homeworkAiService;
    private final SubjectPromptService subjectPromptService;

    @Override
    @TrackTokenUsage
    @NewSpan("homework.solve")
    public SolvedHomework solve(SolveRequest request) {
        Optional<SubjectPrompt> customPrompt = subjectPromptService.findBySpecId(request.specId());

        if (customPrompt.isPresent() && customPrompt.get().getStaticText() != null) {
            log.info("Using static text for specId={}, skipping AI", request.specId());
            return new SolvedHomework(null, null, customPrompt.get().getStaticText());
        }

        DocumentExtractor extractor = strategyResolver.resolve(request.filename());
        ExtractedDocument extracted = extractor.extract(request.content(), request.filename());
        log.debug("Extracted document: filename={}, textLength={}", request.filename(), extracted.text().length());

        String systemPrompt = customPrompt.map(SubjectPrompt::getSystemPrompt).orElse(null);
        if (customPrompt.isPresent()) {
            log.info("Using custom AI prompt for specId={}", request.specId());
        } else {
            log.info("No custom prompt for specId={}, using default", request.specId());
        }

        AiSolveRequest aiRequest = new AiSolveRequest(
                extracted.text(), systemPrompt,
                request.theme(), request.teacherFio(), request.nameSpec(), request.comment(),
                extracted.images());

        return homeworkAiService.solve(aiRequest);
    }
}
