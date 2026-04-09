package com.aquadev.aiexecutor.service.ai.ocr;

import com.aquadev.aiexecutor.exception.domain.OcrFailedException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.ai.content.Media;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class TikaOcrStrategy implements OcrStrategy {

    private static final Set<String> SUPPORTED = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv",
            "application/rtf",
            "application/vnd.oasis.opendocument.text",
            "application/xml",
            "application/epub+zip",
            "application/x-tex",
            "text/x-opml",
            "image/svg+xml"
    );

    private static final int MAX_STRING_LENGTH = -1; // no limit

    private final Tika tika = new Tika();

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED;
    }

    @Override
    public OcrResult extract(byte[] content, String mimeType) {
        log.debug("Extracting text via Tika: mimeType={}, size={} bytes", mimeType, content.length);
        try {
            Metadata metadata = new Metadata();
            String text = tika.parseToString(new ByteArrayInputStream(content), metadata, MAX_STRING_LENGTH);
            log.debug("Tika extraction complete: chars={}", text.length());
            return new OcrResult(text, List.of());
        } catch (Exception e) {
            throw new OcrFailedException("Tika extraction failed for mimeType=" + mimeType + ": " + e.getMessage(), e);
        }
    }
}
