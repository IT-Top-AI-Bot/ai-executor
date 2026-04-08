package com.aquadev.aiexecutor.util;

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
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx";
            case "application/vnd.ms-powerpoint" -> "ppt";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx";
            case "application/vnd.ms-excel" -> "xls";
            case "text/csv" -> "csv";
            case "application/epub+zip" -> "epub";
            case "application/rtf", "text/rtf" -> "rtf";
            case "application/vnd.oasis.opendocument.text" -> "odt";
            case "application/xml", "text/xml" -> "xml";
            case "application/x-fictionbook+xml" -> "fb2";
            case "application/x-ipynb+json" -> "ipynb";
            case "application/x-tex" -> "tex";
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "image/tiff" -> "tiff";
            case "image/avif" -> "avif";
            case "image/heic" -> "heic";
            case "image/heif" -> "heif";
            default -> "txt";
        };
    }
}
