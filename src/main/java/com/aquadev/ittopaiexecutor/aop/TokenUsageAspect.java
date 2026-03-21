package com.aquadev.ittopaiexecutor.aop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TokenUsageAspect {

    private final TokenUsageHolder tokenUsageHolder;

    @Around("@annotation(TrackTokenUsage)")
    public Object track(ProceedingJoinPoint pjp) throws Throwable {
        tokenUsageHolder.init();
        try {
            return pjp.proceed();
        } finally {
            log.info("Token usage — {}", tokenUsageHolder.get());
            tokenUsageHolder.clear();
        }
    }
}
