package com.aquadev.aiexecutor.exception;

import com.aquadev.aiexecutor.dto.ErrorResponse;
import com.aquadev.aiexecutor.dto.ValidationErrorResponse;
import com.aquadev.aiexecutor.exception.base.UnsupportedMediaTypeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final String KEY_BAD_REQUEST = "error.request.bad";
    private static final String KEY_MALFORMED_REQUEST = "error.request.malformed";
    private static final String KEY_VALIDATION_FAILED = "error.request.validation";
    private static final String KEY_INTERNAL_ERROR = "error.internal";

    private final MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors,
                KEY_VALIDATION_FAILED,
                Map.of()
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            BindException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse> validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ValidationErrorResponse(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                validationErrors,
                KEY_VALIDATION_FAILED,
                Map.of()
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse> validationErrors = ex.getConstraintViolations().stream()
                .map(violation -> new ValidationErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Constraint violation",
                request,
                validationErrors,
                KEY_VALIDATION_FAILED,
                Map.of()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed request body",
                request,
                List.of(),
                KEY_MALFORMED_REQUEST,
                Map.of()
        );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        List<ValidationErrorResponse> validationErrors = List.of(
                new ValidationErrorResponse(ex.getParameterName(), "Required request parameter is missing"));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Missing required request parameter",
                request,
                validationErrors,
                KEY_BAD_REQUEST,
                Map.of("parameter", ex.getParameterName())
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        Class<?> type = ex.getRequiredType();
        String requiredType = type != null ? type.getSimpleName() : "unknown";

        List<ValidationErrorResponse> validationErrors = List.of(
                new ValidationErrorResponse(ex.getName(), "Expected type: " + requiredType));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request parameter type mismatch",
                request,
                validationErrors,
                KEY_BAD_REQUEST,
                Map.of("parameter", ex.getName(), "requiredType", requiredType)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage() == null ? "Invalid request" : ex.getMessage(),
                request,
                List.of(),
                KEY_BAD_REQUEST,
                Map.of()
        );
    }

    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            UnsupportedMediaTypeException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                ex.getMessage(),
                request,
                List.of(),
                ex.getI18nKey(),
                ex.getArgs()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        log.debug("ResponseStatusException for path={}: {}", request.getRequestURI(), ex.getReason());
        return buildResponse(status, ex.getReason() != null ? ex.getReason() : status.getReasonPhrase(),
                request, List.of(), null, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception for path={}", request.getRequestURI(), ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                request,
                List.of(),
                KEY_INTERNAL_ERROR,
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ValidationErrorResponse> validationErrors,
            String i18nKey,
            Map<String, Object> args
    ) {
        Map<String, Object> safeArgs = args == null ? Map.of() : Map.copyOf(args);
        List<ValidationErrorResponse> errors = validationErrors == null ? List.of() : validationErrors;
        String userMessage = resolveUserMessage(i18nKey, message, safeArgs);

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                errors.isEmpty() ? null : errors,
                userMessage,
                i18nKey,
                safeArgs.isEmpty() ? null : safeArgs
        );
        return ResponseEntity.status(status).body(body);
    }

    private String resolveUserMessage(String i18nKey, String fallback, Map<String, Object> args) {
        if (i18nKey == null || i18nKey.isBlank()) {
            return fallback;
        }
        Locale locale = LocaleContextHolder.getLocale();
        String template = messageSource.getMessage(i18nKey, null, fallback, locale);
        if (args == null || args.isEmpty()) {
            return template;
        }

        String resolved = Objects.requireNonNullElse(template, Objects.requireNonNullElse(fallback, ""));
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return resolved;
    }
}
