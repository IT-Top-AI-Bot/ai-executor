package com.aquadev.aiexecutor.service.ai.mistral.recovery;

import com.aquadev.aiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles responses with unescaped quotes that break JSON string boundaries.
 * Extracts fields manually via regex and raw string search.
 */
@Slf4j
@Component
@Order(2)
public class ManualFieldExtractionRecoveryStrategy implements RecoveryStrategy {

    private static final Pattern SIMPLE_FIELD_PATTERN =
            Pattern.compile("\"(filename|extension)\"\\s*:\\s*\"([^\"\\\\]*)\"");

    @Override
    public Optional<SolvedHomework> tryRecover(String rawJson) {
        String filename = null;
        String extension = null;

        Matcher m = SIMPLE_FIELD_PATTERN.matcher(rawJson);
        while (m.find()) {
            if ("filename".equals(m.group(1))) filename = m.group(2);
            if ("extension".equals(m.group(1))) extension = m.group(2);
        }

        String content = extractContentValue(rawJson);
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }

        log.warn("Recovered broken JSON manually: filename={}, extension={}, contentLen={}",
                filename, extension, content.length());
        return Optional.of(new SolvedHomework(filename, extension, content));
    }

    @Nullable
    private static String extractContentValue(String rawJson) {
        int keyIdx = rawJson.indexOf("\"content\"");
        if (keyIdx < 0) return null;
        int colon = rawJson.indexOf(':', keyIdx);
        if (colon < 0) return null;
        int openQuote = rawJson.indexOf('"', colon + 1);
        if (openQuote < 0) return null;

        String value = rawJson.substring(openQuote + 1).stripTrailing();
        if (value.endsWith("}")) value = value.substring(0, value.length() - 1).stripTrailing();
        if (value.endsWith("\"")) value = value.substring(0, value.length() - 1).stripTrailing();

        return value
                .replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
