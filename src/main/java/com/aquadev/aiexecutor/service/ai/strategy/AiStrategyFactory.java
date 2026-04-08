package com.aquadev.aiexecutor.service.ai.strategy;

import com.aquadev.aiexecutor.dto.AiProviderType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiStrategyFactory {

    private final Map<AiProviderType, AiSolveStrategy> solveStrategies;

    private final Map<AiProviderType, AiVisionStrategy> visionStrategies;

    public AiStrategyFactory(List<AiSolveStrategy> solveList, List<AiVisionStrategy> visionList) {
        solveStrategies = solveList.stream()
                .collect(Collectors.toMap(AiSolveStrategy::providerType, Function.identity()));
        visionStrategies = visionList.stream()
                .collect(Collectors.toMap(AiVisionStrategy::providerType, Function.identity()));
    }

    public AiSolveStrategy getSolveStrategy(AiProviderType type) {
        return Optional.ofNullable(solveStrategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("No solve strategy for provider: " + type));
    }

    public AiVisionStrategy getVisionStrategy(AiProviderType type) {
        return Optional.ofNullable(visionStrategies.get(type))
                .orElseThrow(() -> new IllegalArgumentException("No vision strategy for provider: " + type));
    }
}
