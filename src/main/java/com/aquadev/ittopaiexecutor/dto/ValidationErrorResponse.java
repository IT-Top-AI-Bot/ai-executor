package com.aquadev.ittopaiexecutor.dto;

public record ValidationErrorResponse(
        String field,
        String message
) {
}
