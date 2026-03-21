package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Component
public class TxtDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        String text = new String(content, StandardCharsets.UTF_8);
        return new ExtractedDocument(text, Map.of("source", filename));
    }
}
