package com.aquadev.aiexecutor.dto;

import org.springframework.ai.chat.metadata.Usage;

public class TokenUsage {

    private int textPromptTokens;
    private int textCompletionTokens;

    public void addText(Usage usage) {
        if (usage == null) return;
        textPromptTokens += usage.getPromptTokens();
        textCompletionTokens += usage.getCompletionTokens();
    }

    public int getPromptTokens() {
        return textPromptTokens;
    }

    public int getCompletionTokens() {
        return textCompletionTokens;
    }

    public int getTotalTokens() {
        return textPromptTokens + textCompletionTokens;
    }

    @Override
    public String toString() {
        int textTotal = textPromptTokens + textCompletionTokens;
        return """
                text(prompt=%d, completion=%d, total=%d) | \
                total=%d""".formatted(
                textPromptTokens, textCompletionTokens, textTotal,
                getTotalTokens());
    }
}
