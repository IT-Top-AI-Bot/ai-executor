package com.aquadev.aiexecutor.service.ai.strategy;

import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import org.springframework.ai.content.Media;

import java.util.List;

public interface AiSolveStrategy {

    SolvedHomework executeSolveRequest(String systemPrompt, String userMessage, List<Media> images);

    AiProviderType providerType();
}
