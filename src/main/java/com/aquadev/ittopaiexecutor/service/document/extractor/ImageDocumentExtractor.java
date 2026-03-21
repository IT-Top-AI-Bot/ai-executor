package com.aquadev.ittopaiexecutor.service.document.extractor;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Обрабатывает растровые изображения как задание:
 * передаёт файл напрямую в vision-модель без предварительного OCR.
 * Поддерживаемые форматы: PNG, JPG/JPEG, GIF, WebP, BMP, TIFF.
 */
@Component
public class ImageDocumentExtractor implements DocumentExtractor {

    private static final Map<String, MimeType> EXTENSION_MIME = Map.of(
            "png", MimeTypeUtils.IMAGE_PNG,
            "jpg", MimeTypeUtils.IMAGE_JPEG,
            "jpeg", MimeTypeUtils.IMAGE_JPEG,
            "gif", MimeTypeUtils.IMAGE_GIF,
            "webp", MimeType.valueOf("image/webp"),
            "bmp", MimeType.valueOf("image/bmp"),
            "tiff", MimeType.valueOf("image/tiff"),
            "tif", MimeType.valueOf("image/tiff")
    );

    @Override
    public Set<String> supportedMimeTypes() {
        return EXTENSION_MIME.keySet();
    }

    @Override
    public ExtractedDocument extract(byte[] content, String filename) {
        String ext = extension(filename);
        MimeType mimeType = EXTENSION_MIME.getOrDefault(ext, MimeTypeUtils.IMAGE_JPEG);
        Media image = new Media(mimeType, new ByteArrayResource(content));
        return new ExtractedDocument("", Map.of("source", filename), List.of(image));
    }

    private static String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
