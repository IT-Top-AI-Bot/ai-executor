package com.aquadev.aiexecutor.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        List<ValidationErrorResponse> errors,
        String userMessage,
        String i18nKey,
        Map<String, Object> args
) {
}
