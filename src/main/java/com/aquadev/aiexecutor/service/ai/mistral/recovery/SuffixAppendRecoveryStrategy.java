package com.aquadev.aiexecutor.service.ai.mistral.recovery;

import com.aquadev.aiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Handles truncated responses by appending closing JSON characters.
 */
@Slf4j
@Component
@Order(1)
public class SuffixAppendRecoveryStrategy implements RecoveryStrategy {

    private final BeanOutputConverter<SolvedHomework> converter =
            new BeanOutputConverter<>(SolvedHomework.class);

    @Override
    public Optional<SolvedHomework> tryRecover(String rawJson) {
        for (String suffix : new String[]{"\"}", "}"}) {
            try {
                SolvedHomework result = converter.convert(rawJson + suffix);
                if (result != null && result.content() != null) {
                    return Optional.of(result);
                }
            } catch (Exception e) {
                log.trace("SuffixAppend with suffix='{}' failed: {}", suffix, e.getMessage());
            }
        }
        return Optional.empty();
    }
}
