package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
abstract class BaseOfficeDocumentExtractor implements DocumentExtractor {

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        String text = extractText(content, filename);
        List<Resource> imageResources = extractImageResources(content, filename);

        if (imageResources.isEmpty()) {
            return new ExtractedDocument(text, Map.of("source", filename));
        }

        List<Media> images = imageResources.stream()
                .map(img -> new Media(detectMime(img), img))
                .toList();

        log.debug("Extracted {} images from {}, will pass directly to generation step",
                images.size(), formatName());
        return new ExtractedDocument(text, Map.of("source", filename, "images", images.size()), images);
    }

    protected abstract String formatName();

    protected abstract List<Resource> extractImageResources(byte[] content, String filename);

    protected String extractText(byte[] content, String filename) {
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

    protected MimeType detectMime(Resource resource) {
        String name = resource.getFilename();
        if (name != null && name.toLowerCase().endsWith(".png")) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        return MimeTypeUtils.IMAGE_JPEG;
    }
}
