package com.aquadev.aiexecutor.dto;

public record ToolResult(
        boolean success,
        String fileName,
        String s3Key,
        String format,
        String errorMessage
) {
}
