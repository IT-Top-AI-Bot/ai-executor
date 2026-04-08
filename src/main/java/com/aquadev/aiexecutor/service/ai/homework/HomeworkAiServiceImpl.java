package com.aquadev.aiexecutor.service.ai.homework;

import com.aquadev.aiexecutor.dto.AiSolveRequest;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.service.ai.strategy.AiProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkAiServiceImpl implements HomeworkAiService {

    private final AiProviderRouter providerRouter;
    private final HomeworkPromptBuilder promptBuilder;

    @Override
    public SolvedHomework solve(AiSolveRequest request) {
        boolean hasImages = request.images() != null && !request.images().isEmpty();
        log.info("Solving homework, hasImages={}", hasImages);

        return providerRouter.executeSolve(
                promptBuilder.buildSystemPrompt(request.systemPrompt()),
                promptBuilder.buildUserMessage(request),
                request.images()
        );
    }
}
