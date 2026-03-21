package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.service.ai.MistralOcrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationFileAttachment;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED = Set.of("pdf");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg");

    private final MistralOcrService mistralOcrService;

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        MistralOcrService.OcrResult ocr = mistralOcrService.extract(content);
        EmbeddedFiles embedded = extractEmbeddedFiles(content);

        String fullText = embedded.text().isBlank()
                ? ocr.text()
                : ocr.text() + "\n\n[Вложенные файлы]\n" + embedded.text();

        List<Media> allImages = new ArrayList<>(ocr.images());
        allImages.addAll(embedded.images());

        log.debug("PDF extracted via OCR: filename={}, totalLength={}, ocrImages={}, attachedImages={}",
                filename, fullText.length(), ocr.images().size(), embedded.images().size());
        return new ExtractedDocument(fullText.trim(), Map.of("source", filename), allImages);
    }


    private EmbeddedFiles extractEmbeddedFiles(byte[] pdfBytes) {
        StringBuilder txtContent = new StringBuilder();
        List<Media> images = new ArrayList<>();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            extractFromNameTree(doc.getDocumentCatalog().getNames(), txtContent, images);
            extractFromPageAnnotations(doc, images);
        } catch (IOException e) {
            log.warn("Could not extract embedded files from PDF: {}", e.getMessage());
        }

        return new EmbeddedFiles(txtContent.toString().trim(), images);
    }

    private void extractFromNameTree(
            PDDocumentNameDictionary names,
            StringBuilder txtContent,
            List<Media> images
    ) throws IOException {
        if (names == null) return;
        PDEmbeddedFilesNameTreeNode tree = names.getEmbeddedFiles();
        if (tree == null) return;
        Map<String, PDComplexFileSpecification> fileMap = tree.getNames();
        if (fileMap == null) return;

        for (Map.Entry<String, PDComplexFileSpecification> entry : fileMap.entrySet()) {
            String name = entry.getKey();
            if (name == null) continue;
            if (name.toLowerCase().endsWith(".txt")) {
                extractTxtContent(name, entry.getValue(), txtContent);
            } else {
                extractImageFromSpec(name, entry.getValue(), images);
            }
        }
    }

    private void extractFromPageAnnotations(PDDocument doc, List<Media> images) throws IOException {
        for (PDPage page : doc.getPages()) {
            for (PDAnnotation ann : page.getAnnotations()) {
                if (ann instanceof PDAnnotationFileAttachment fileAnn) {
                    PDComplexFileSpecification spec = (PDComplexFileSpecification) fileAnn.getFile();
                    if (spec != null) {
                        extractImageFromSpec(spec.getFilename(), spec, images);
                    }
                }
            }
        }
    }

    private void extractTxtContent(String name, PDComplexFileSpecification spec, StringBuilder target) {
        PDEmbeddedFile embeddedFile = spec.getEmbeddedFile();
        if (embeddedFile == null) return;
        try {
            String text = new String(embeddedFile.toByteArray(), StandardCharsets.UTF_8);
            target.append("[Вложение: ").append(name).append("]\n")
                    .append(text).append("\n\n");
            log.debug("Extracted embedded .txt from PDF: {}", name);
        } catch (IOException e) {
            log.warn("Could not read embedded .txt file {}: {}", name, e.getMessage());
        }
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

    private record EmbeddedFiles(String text, List<Media> images) {
    }
}
