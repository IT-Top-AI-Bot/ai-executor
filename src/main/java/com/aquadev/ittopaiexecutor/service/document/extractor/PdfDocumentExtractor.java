package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.service.ai.MistralOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

/**
 * Извлекает содержимое PDF двумя способами:
 * 1. Mistral OCR API (mistral-ocr-latest) — текст, таблицы, изображения, макеты
 * 2. PDFBox — вложенные .txt-файлы из аннотаций PDF (edge case)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED = Set.of("pdf");

    private final MistralOcrService mistralOcrService;

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename, ChatClient chatClient) {
        String ocrText      = mistralOcrService.extractText(content);
        String embeddedText = extractEmbeddedTxtFiles(content);

        String fullText = embeddedText.isBlank()
                ? ocrText
                : ocrText + "\n\n[Вложенные файлы]\n" + embeddedText;

        log.debug("PDF extracted via OCR: filename={}, totalLength={}", filename, fullText.length());
        return new ExtractedDocument(fullText.trim(), Map.of("source", filename));
    }

    // ─── Embedded .txt files (PDFBox) ─────────────────────────────────────────

    private String extractEmbeddedTxtFiles(byte[] pdfBytes) {
        StringBuilder sb = new StringBuilder();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            if (names == null) return "";

            PDEmbeddedFilesNameTreeNode tree = names.getEmbeddedFiles();
            if (tree == null) return "";

            Map<String, PDComplexFileSpecification> fileMap = tree.getNames();
            if (fileMap == null || fileMap.isEmpty()) return "";

            for (Map.Entry<String, PDComplexFileSpecification> entry : fileMap.entrySet()) {
                String embName = entry.getKey();
                if (embName != null && embName.toLowerCase().endsWith(".txt")) {
                    PDEmbeddedFile embeddedFile = entry.getValue().getEmbeddedFile();
                    if (embeddedFile != null) {
                        String txtContent = new String(embeddedFile.toByteArray(), StandardCharsets.UTF_8);
                        sb.append("[Вложение: ").append(embName).append("]\n")
                          .append(txtContent).append("\n\n");
                        log.debug("Extracted embedded .txt from PDF: {}", embName);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not extract embedded files from PDF: {}", e.getMessage());
        }
        return sb.toString().trim();
    }
}
