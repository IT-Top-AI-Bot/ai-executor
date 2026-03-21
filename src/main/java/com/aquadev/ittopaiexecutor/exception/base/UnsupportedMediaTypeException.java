package com.aquadev.ittopaiexecutor.exception.base;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@Getter
@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class UnsupportedMediaTypeException extends RuntimeException {

    private final String i18nKey;
    private final transient Map<String, Object> args;

    protected UnsupportedMediaTypeException(String message, String i18nKey, Map<String, Object> args) {
        super(message);
        this.i18nKey = i18nKey;
        this.args = args == null ? Map.of() : Map.copyOf(args);
    }
}
