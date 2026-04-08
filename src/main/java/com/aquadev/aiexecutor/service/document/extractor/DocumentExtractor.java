package com.aquadev.aiexecutor.service.document.extractor;

import com.aquadev.aiexecutor.dto.ExtractedDocument;

import java.util.Set;

public interface DocumentExtractor {

    Set<String> supportedMimeTypes();

    ExtractedDocument extract(byte[] content, String filename);
}
