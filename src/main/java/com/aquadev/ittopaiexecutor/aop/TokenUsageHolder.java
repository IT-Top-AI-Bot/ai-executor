package com.aquadev.ittopaiexecutor.aop;

import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import org.springframework.stereotype.Component;

@Component
public class TokenUsageHolder {

    private final ThreadLocal<TokenUsage> holder = new ThreadLocal<>();

    public void init() {
        holder.set(new TokenUsage());
    }

    public TokenUsage get() {
        TokenUsage usage = holder.get();
        if (usage == null) {
            throw new IllegalStateException("TokenUsage not initialized — use @TrackTokenUsage");
        }
        return usage;
    }

    public void clear() {
        holder.remove();
    }
}
