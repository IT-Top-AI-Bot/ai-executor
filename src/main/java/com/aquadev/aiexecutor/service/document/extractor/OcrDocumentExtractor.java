package com.aquadev.aiexecutor.service.document.extractor;

import com.aquadev.aiexecutor.dto.ExtractedDocument;
import com.aquadev.aiexecutor.service.ai.ocr.OcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcrDocumentExtractor implements DocumentExtractor {

    private static final Map<String, String> EXTENSION_TO_MIME = Map.ofEntries(
            // PDF
            Map.entry("pdf", "application/pdf"),
            // Word Documents
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("doc", "application/msword"),
            // PowerPoint
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            // Excel
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("csv", "text/csv"),
            // Other documents
            Map.entry("epub", "application/epub+zip"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("xml", "application/xml"),
            Map.entry("bib", "application/x-bibtex"),
            Map.entry("fb2", "application/x-fictionbook+xml"),
            Map.entry("ipynb", "application/x-ipynb+json"),
            Map.entry("tex", "application/x-tex"),
            Map.entry("opml", "text/x-opml"),
            Map.entry("1", "application/x-troff"),
            Map.entry("man", "application/x-troff-man"),
            // Images
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("bmp", "image/bmp"),
            Map.entry("tiff", "image/tiff"),
            Map.entry("tif", "image/tiff"),
            Map.entry("avif", "image/avif"),
            Map.entry("heic", "image/heic"),
            Map.entry("heif", "image/heif")
    );

    private final OcrService ocrService;

    @Override
    public Set<String> supportedMimeTypes() {
        return EXTENSION_TO_MIME.keySet();
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        String ext = filename.contains(".")
                ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase()
                : "";
        String mimeType = EXTENSION_TO_MIME.getOrDefault(ext, "application/octet-stream");

        log.info("Extracting via OCR: filename={}, mimeType={}, size={} bytes", filename, mimeType, content.length);

        OcrService.OcrResult ocr = ocrService.extract(content, mimeType);

        log.info("OCR completed: filename={}, textLength={}, images={}", filename, ocr.text().length(), ocr.images().size());
        return new ExtractedDocument(ocr.text().trim(), Map.of("source", filename), ocr.images());
    }
}
