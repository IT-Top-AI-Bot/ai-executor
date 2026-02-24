package com.aquadev.ittopaiexecutor.exception.base;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
public class UnsupportedMediaTypeException extends RuntimeException {

    protected UnsupportedMediaTypeException(String message, String i18nKey, Map<String, Object> args) {
        super(message);
    }
}
