package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DocxDocumentExtractor implements DocumentExtractor {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("docx", "doc");

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename, ChatClient chatClient) {
        String text = extractText(content, filename);
        List<Resource> imageResources = extractImageResources(content, filename);

        if (imageResources.isEmpty()) {
            return new ExtractedDocument(text, Map.of("source", filename));
        }

        List<Media> images = imageResources.stream()
                .map(img -> new Media(detectMime(img), img))
                .toList();

        log.debug("Extracted {} images from DOCX, will pass directly to generation step", images.size());
        return new ExtractedDocument(text, Map.of("source", filename, "images", images.size()), images);
    }

    private String extractText(byte[] content, String filename) {
        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new TikaDocumentReader(resource).read().stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));
    }

    private List<Resource> extractImageResources(byte[] content, String filename) {
        if (filename.toLowerCase().endsWith(".doc")) {
            // OLE2 format — XWPFDocument is OOXML only; skip image extraction
            log.debug("Skipping image extraction for OLE2 .doc file: {}", filename);
            return List.of();
        }
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            return doc.getAllPictures().stream()
                    .map(pic -> (Resource) new ByteArrayResource(pic.getData()) {
                        @Override
                        public String getFilename() {
                            return pic.getFileName();
                        }
                    })
                    .toList();
        } catch (IOException e) {
            log.warn("Could not extract images from DOCX: {}", e.getMessage());
            return List.of();
        }
    }

    private MimeType detectMime(Resource resource) {
        String name = resource.getFilename();
        if (name != null && name.toLowerCase().endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }
}
