package com.aquadev.aiexecutor.service.document;

import com.aquadev.aiexecutor.exception.domain.UnsupportedDocumentTypeException;
import com.aquadev.aiexecutor.service.document.extractor.DocumentExtractor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DocumentStrategyResolver {

    private Map<String, DocumentExtractor> registry;
    private final List<DocumentExtractor> strategies;

    @PostConstruct
    void init() {
        registry = new HashMap<>();
        strategies.forEach(s ->
                s.supportedMimeTypes().forEach(m -> registry.put(m, s))
        );
    }

    public DocumentExtractor resolve(String filename) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        DocumentExtractor extractor = registry.get(ext);
        if (extractor == null) {
            throw new UnsupportedDocumentTypeException(
                    "Unsupported format: .%s. Supported: %s".formatted(ext, registry.keySet()),
                    ext
            );
        }
        return extractor;
    }
}


