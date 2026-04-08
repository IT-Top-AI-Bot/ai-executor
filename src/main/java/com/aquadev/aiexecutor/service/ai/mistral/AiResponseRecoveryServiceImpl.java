package com.aquadev.aiexecutor.service.ai.mistral;

import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.service.ai.mistral.recovery.RecoveryStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiResponseRecoveryServiceImpl implements AiResponseRecoveryService {

    private final List<RecoveryStrategy> strategies;

    @Override
    public SolvedHomework tryRecover(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return null;
        return strategies.stream()
                .map(s -> s.tryRecover(rawJson))
                .filter(opt -> opt.isPresent() && opt.get().content() != null)
                .map(opt -> opt.get())
                .findFirst()
                .orElse(null);
    }
}
