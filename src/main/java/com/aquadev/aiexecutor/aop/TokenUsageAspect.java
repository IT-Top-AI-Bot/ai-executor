package com.aquadev.aiexecutor.aop;

import com.aquadev.aiexecutor.dto.SolveRequest;
import com.aquadev.aiexecutor.dto.TokenUsage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TokenUsageAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(TrackTokenUsage)")
    public Object track(ProceedingJoinPoint pjp) throws Throwable {
        String specId = Arrays.stream(pjp.getArgs())
                .filter(SolveRequest.class::isInstance)
                .map(a -> String.valueOf(((SolveRequest) a).specId()))
                .findFirst()
                .orElse("unknown");

        TokenUsage usage = new TokenUsage();
        return ScopedValue.where(TokenUsageHolder.SCOPED_USAGE, usage).call(() -> {
            try {
                Object result = pjp.proceed();
                recordTokenMetrics(specId, usage);
                counter("ai.requests", specId, "success").increment();
                return result;
            } catch (Exception e) {
                counter("ai.requests", specId, "failure").increment();
                throw e;
            } finally {
                log.info("Token usage — {}", usage);
            }
        });
    }

    private void recordTokenMetrics(String specId, TokenUsage usage) {
        Counter.builder("ai.tokens")
                .tag("type", "prompt")
                .tag("specId", specId)
                .description("AI prompt tokens consumed")
                .register(meterRegistry)
                .increment(usage.getPromptTokens());
        Counter.builder("ai.tokens")
                .tag("type", "completion")
                .tag("specId", specId)
                .description("AI completion tokens consumed")
                .register(meterRegistry)
                .increment(usage.getCompletionTokens());
    }

    private Counter counter(String name, String specId, String outcome) {
        return Counter.builder(name)
                .tag("specId", specId)
                .tag("outcome", outcome)
                .description("AI homework solve requests")
                .register(meterRegistry);
    }
}
