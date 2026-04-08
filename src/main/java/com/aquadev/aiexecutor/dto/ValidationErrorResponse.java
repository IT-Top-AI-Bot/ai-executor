package com.aquadev.aiexecutor.dto;

public record ValidationErrorResponse(
        String field,
        String message
) {
}
