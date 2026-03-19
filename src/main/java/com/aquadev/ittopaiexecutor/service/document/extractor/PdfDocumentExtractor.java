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
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
        MistralOcrService.OcrResult ocr = mistralOcrService.extract(content);
        String embeddedText = extractEmbeddedTxtFiles(content);
        List<Media> embeddedImages = extractEmbeddedImageFiles(content);

        String fullText = embeddedText.isBlank()
                ? ocr.text()
                : ocr.text() + "\n\n[Вложенные файлы]\n" + embeddedText;

        List<Media> allImages = new ArrayList<>(ocr.images());
        allImages.addAll(embeddedImages);

        log.debug("PDF extracted via OCR: filename={}, totalLength={}, ocrImages={}, attachedImages={}",
                filename, fullText.length(), ocr.images().size(), embeddedImages.size());
        return new ExtractedDocument(fullText.trim(), Map.of("source", filename), allImages);
    }

    // ─── Embedded image files (PDFBox) ────────────────────────────────────────

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg");

    private List<Media> extractEmbeddedImageFiles(byte[] pdfBytes) {
        List<Media> result = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            // 1. Document-level name tree attachments
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            if (names != null) {
                PDEmbeddedFilesNameTreeNode tree = names.getEmbeddedFiles();
                if (tree != null) {
                    Map<String, PDComplexFileSpecification> fileMap = tree.getNames();
                    if (fileMap != null) {
                        for (Map.Entry<String, PDComplexFileSpecification> entry : fileMap.entrySet()) {
                            extractImageFromSpec(entry.getKey(), entry.getValue(), result);
                        }
                    }
                }
            }

            // 2. Page annotation attachments (скрепки в Adobe Reader)
            for (PDPage page : doc.getPages()) {
                for (PDAnnotation ann : page.getAnnotations()) {
                    if (ann instanceof PDAnnotationFileAttachment fileAnn) {
                        PDComplexFileSpecification spec = (PDComplexFileSpecification) fileAnn.getFile();
                        if (spec != null) {
                            extractImageFromSpec(spec.getFilename(), spec, result);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Could not extract embedded images from PDF: {}", e.getMessage());
        }
        return result;
    }

    private void extractImageFromSpec(String name, PDComplexFileSpecification spec, List<Media> result) {
        if (name == null) return;
        String ext = name.contains(".")
                ? name.substring(name.lastIndexOf('.') + 1).toLowerCase()
                : "";
        if (!IMAGE_EXTENSIONS.contains(ext)) return;

        PDEmbeddedFile embeddedFile = spec.getEmbeddedFile();
        if (embeddedFile == null) return;

        try {
            byte[] imgBytes = embeddedFile.toByteArray();
            result.add(new Media(detectImageMime(ext), new ByteArrayResource(imgBytes)));
            log.debug("Extracted embedded image from PDF: name={}, size={}", name, imgBytes.length);
        } catch (IOException e) {
            log.warn("Could not read embedded image {}: {}", name, e.getMessage());
        }
    }

    private MimeType detectImageMime(String ext) {
        return switch (ext) {
            case "png" -> MimeTypeUtils.IMAGE_PNG;
            case "gif" -> MimeTypeUtils.IMAGE_GIF;
            case "svg" -> MimeType.valueOf("image/svg+xml");
            default -> MimeTypeUtils.IMAGE_JPEG;
        };
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
