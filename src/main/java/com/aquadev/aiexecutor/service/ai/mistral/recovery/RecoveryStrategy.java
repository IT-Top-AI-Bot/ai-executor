package com.aquadev.aiexecutor.service.ai.mistral.recovery;

import com.aquadev.aiexecutor.dto.SolvedHomework;

import java.util.Optional;

public interface RecoveryStrategy {
    Optional<SolvedHomework> tryRecover(String rawJson);
}
