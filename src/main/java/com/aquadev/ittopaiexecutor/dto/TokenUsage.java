package com.aquadev.ittopaiexecutor.dto;

import lombok.Getter;
import org.springframework.ai.chat.metadata.Usage;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TokenUsage {

    private final AtomicInteger visionPromptTokens = new AtomicInteger(0);
    private final AtomicInteger visionCompletionTokens = new AtomicInteger(0);

    private final AtomicInteger textPromptTokens = new AtomicInteger(0);
    private final AtomicInteger textCompletionTokens = new AtomicInteger(0);

    public void addVision(Usage usage) {
        if (usage == null) return;
        visionPromptTokens.addAndGet(usage.getPromptTokens());
        visionCompletionTokens.addAndGet(usage.getCompletionTokens());
    }

    public void addText(Usage usage) {
        if (usage == null) return;
        textPromptTokens.addAndGet(usage.getPromptTokens());
        textCompletionTokens.addAndGet(usage.getCompletionTokens());
    }

    public int getTotalTokens() {
        return visionPromptTokens.get() + visionCompletionTokens.get()
                + textPromptTokens.get() + textCompletionTokens.get();
    }

    @Override
    public String toString() {
        int visionTotal = visionPromptTokens.get() + visionCompletionTokens.get();
        int textTotal = textPromptTokens.get() + textCompletionTokens.get();
        return """
                vision(prompt=%d, completion=%d, total=%d) | \
                text(prompt=%d, completion=%d, total=%d) | \
                total=%d""".formatted(
                visionPromptTokens.get(), visionCompletionTokens.get(), visionTotal,
                textPromptTokens.get(), textCompletionTokens.get(), textTotal,
                getTotalTokens());
    }
}
