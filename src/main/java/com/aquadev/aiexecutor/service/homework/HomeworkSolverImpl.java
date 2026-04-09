package com.aquadev.aiexecutor.service.homework;

import com.aquadev.aiexecutor.aop.TrackTokenUsage;
import com.aquadev.aiexecutor.dto.AiSolveRequest;
import com.aquadev.aiexecutor.dto.ExtractedDocument;
import com.aquadev.aiexecutor.dto.SolveRequest;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.model.SubjectPrompt;
import com.aquadev.aiexecutor.service.ai.homework.HomeworkAiService;
import com.aquadev.aiexecutor.service.ai.homework.HomeworkContextBuilder;
import com.aquadev.aiexecutor.service.document.DocumentStrategyResolver;
import com.aquadev.aiexecutor.service.document.extractor.DocumentExtractor;
import com.aquadev.aiexecutor.service.extraction.HomeworkExtractionCacheService;
import com.aquadev.aiexecutor.service.prompt.SubjectPromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkSolverImpl implements HomeworkSolver {

    private final DocumentStrategyResolver strategyResolver;
    private final HomeworkAiService homeworkAiService;
    private final HomeworkContextBuilder contextBuilder;
    private final SubjectPromptService subjectPromptService;
    private final HomeworkExtractionCacheService extractionCacheService;

    @Override
    @TrackTokenUsage
    public SolvedHomework solve(SolveRequest request) {
        Optional<SubjectPrompt> customPrompt = request.telegramUserId() != null
                ? subjectPromptService.findByTelegramUserIdAndSpecId(request.telegramUserId(), request.specId())
                : Optional.empty();

        if (customPrompt.isPresent() && customPrompt.get().getStaticText() != null) {
            log.info("Using static text for specId={}, skipping AI", request.specId());
            return new SolvedHomework(null, null, customPrompt.get().getStaticText());
        }

        ExtractedDocument extracted = resolveExtraction(request);
        log.info("Extracted document: filename={}, textLength={}, images={}",
                request.filename(), extracted.text().length(), extracted.images().size());

        String systemPrompt = customPrompt.map(SubjectPrompt::getSystemPrompt).orElse(null);
        if (customPrompt.isPresent()) {
            log.info("Using custom AI prompt for specId={}", request.specId());
        } else {
            log.info("No custom prompt for specId={}, using default", request.specId());
        }

        AiSolveRequest aiRequest = new AiSolveRequest(
                extracted.text(), systemPrompt,
                request.theme(), request.teacherFio(), request.nameSpec(), request.comment(),
                List.of());

        return homeworkAiService.solve(aiRequest);
    }

    private ExtractedDocument resolveExtraction(SolveRequest request) {
        String documentHash = computeHash(request.content());

        if (request.homeworkId() != null) {
            Optional<ExtractedDocument> cached = extractionCacheService.findCached(request.homeworkId(), documentHash);
            if (cached.isPresent()) {
                log.info("Extraction cache hit for homeworkId={}", request.homeworkId());
                return cached.get();
            }
        }

        DocumentExtractor extractor = strategyResolver.resolve(request.filename());
        ExtractedDocument extracted = extractor.extract(request.content(), request.filename());

        String fullContext = contextBuilder.buildFullContext(extracted.text(), extracted.images());
        ExtractedDocument enriched = new ExtractedDocument(fullContext, extracted.metadata());

        if (request.homeworkId() != null) {
            extractionCacheService.saveOrUpdate(request.homeworkId(), documentHash, enriched);
            log.debug("Extraction cached for homeworkId={}", request.homeworkId());
        }

        return enriched;
    }

    private static String computeHash(byte[] content) {
        return DigestUtils.md5DigestAsHex(content);
    }
}
