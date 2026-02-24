package com.aquadev.ittopaiexecutor.exception.domain;

import com.aquadev.ittopaiexecutor.exception.base.UnsupportedMediaTypeException;

import java.util.Map;

public class UnsupportedDocumentTypeException extends UnsupportedMediaTypeException {

    public UnsupportedDocumentTypeException(String message, String type) {
        super(message, "error.document.unsupportedType", Map.of("type", type));
    }
}
