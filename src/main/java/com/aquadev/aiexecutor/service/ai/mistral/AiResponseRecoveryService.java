package com.aquadev.aiexecutor.service.ai.mistral;

import com.aquadev.aiexecutor.dto.SolvedHomework;

public interface AiResponseRecoveryService {

    /**
     * Attempts to recover a {@link SolvedHomework} from a broken or truncated JSON response.
     *
     * @return recovered instance, or {@code null} if recovery failed
     */
    SolvedHomework tryRecover(String rawJson);
}
