package com.aquadev.ittopaiexecutor.util;

import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class FileTypeDetector {

    private static final Tika TIKA = new Tika();

    public String detectExtension(byte[] content) {
        String mimeType = TIKA.detect(content);
        return extensionForMimeType(mimeType);
    }

    private String extensionForMimeType(String mimeType) {
        return switch (mimeType) {
            case "application/pdf" -> "pdf";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "application/msword" -> "doc";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            default -> "txt";
        };
    }
}
