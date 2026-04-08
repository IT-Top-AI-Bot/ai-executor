package com.aquadev.aiexecutor.exception.domain;

import com.aquadev.aiexecutor.exception.base.UnsupportedMediaTypeException;

import java.util.Map;

public class UnsupportedDocumentTypeException extends UnsupportedMediaTypeException {

    public UnsupportedDocumentTypeException(String message, String type) {
        super(message, "error.document.unsupportedType", Map.of("type", type));
    }
}
