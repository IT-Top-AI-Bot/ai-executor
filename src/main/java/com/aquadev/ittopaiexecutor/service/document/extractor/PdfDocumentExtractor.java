package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class PdfDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("pdf");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename, ChatClient chatClient) {
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        String text = chatClient.prompt()
                .user(u -> u
                        .text("Extract and return the complete text content of this document. " +
                                "If there are images or screenshots — describe their content inline.")
                        .media(new Media(new MimeType("application", "pdf"), resource))
                )
                .call()
                .content();

        String embeddedText = extractEmbeddedTxtFiles(content);
        if (!embeddedText.isBlank()) {
            text = text + "\n\n" + embeddedText;
        }

        return new ExtractedDocument(text, Map.of("source", filename));
    }

    private String extractEmbeddedTxtFiles(byte[] pdfBytes) {
        StringBuilder sb = new StringBuilder();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            PDDocumentCatalog catalog = doc.getDocumentCatalog();
            PDDocumentNameDictionary names = catalog.getNames();
            if (names == null) {
                return "";
            }
            PDEmbeddedFilesNameTreeNode embeddedFilesTree = names.getEmbeddedFiles();
            if (embeddedFilesTree == null) {
                return "";
            }

            Map<String, PDComplexFileSpecification> fileMap = embeddedFilesTree.getNames();
            if (fileMap == null || fileMap.isEmpty()) {
                return "";
            }

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
