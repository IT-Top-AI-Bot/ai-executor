package com.aquadev.aiexecutor.service.document.extractor;

import com.aquadev.aiexecutor.dto.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class TxtDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED_TYPES = Set.of("txt");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        log.info("Extracting plain text: filename={}, size={} bytes", filename, content.length);
        String text = new String(content, StandardCharsets.UTF_8);
        log.info("Plain text extracted: filename={}, textLength={}", filename, text.length());
        return new ExtractedDocument(text, Map.of("source", filename));
    }
}
