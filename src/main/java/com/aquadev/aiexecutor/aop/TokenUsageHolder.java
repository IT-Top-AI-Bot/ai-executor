package com.aquadev.aiexecutor.aop;

import com.aquadev.aiexecutor.dto.TokenUsage;
import org.springframework.stereotype.Component;

@Component
public class TokenUsageHolder {

    public static final ScopedValue<TokenUsage> SCOPED_USAGE = ScopedValue.newInstance();

    public TokenUsage get() {
        if (!SCOPED_USAGE.isBound()) {
            throw new IllegalStateException("TokenUsage not initialized — use @TrackTokenUsage");
        }
        return SCOPED_USAGE.get();
    }
}
