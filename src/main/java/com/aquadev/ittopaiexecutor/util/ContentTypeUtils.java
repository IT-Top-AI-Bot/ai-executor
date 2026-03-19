package com.aquadev.ittopaiexecutor.util;

import org.springframework.lang.Nullable;

import java.util.Map;

public final class ContentTypeUtils {

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("html",  "text/html; charset=utf-8"),
            Map.entry("htm",   "text/html; charset=utf-8"),
            Map.entry("css",   "text/css; charset=utf-8"),
            Map.entry("js",    "application/javascript"),
            Map.entry("ts",    "application/typescript"),
            Map.entry("py",    "text/x-python; charset=utf-8"),
            Map.entry("java",  "text/x-java-source; charset=utf-8"),
            Map.entry("txt",   "text/plain; charset=utf-8"),
            Map.entry("md",    "text/markdown; charset=utf-8"),
            Map.entry("json",  "application/json"),
            Map.entry("xml",   "application/xml"),
            Map.entry("csv",   "text/csv; charset=utf-8"),
            Map.entry("pdf",   "application/pdf"),
            Map.entry("docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("doc",   "application/msword"),
            Map.entry("xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("zip",   "application/zip"),
            Map.entry("png",   "image/png"),
            Map.entry("jpg",   "image/jpeg"),
            Map.entry("jpeg",  "image/jpeg")
    );

    private static final Map<String, String> BY_CONTENT_TYPE = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/msword", "doc",
            "text/plain", "txt"
    );

    private ContentTypeUtils() {
    }

    /**
     * Returns the MIME content type for a given file extension (without leading dot).
     * Returns {@code "application/octet-stream"} for unknown extensions.
     */
    public static String forExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "application/octet-stream";
        }
        return BY_EXTENSION.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    /**
     * Extracts the extension from a filename and returns the corresponding content type.
     */
    public static String forFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        return forExtension(ext);
    }

    /**
     * Returns the file extension for a given Content-Type value (ignoring parameters).
     * Returns {@code null} for unknown or unsupported content types.
     * Example: {@code "application/pdf"} → {@code "pdf"}.
     */
    @Nullable
    public static String extensionForContentType(@Nullable String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        String mediaType = contentType.split(";")[0].trim().toLowerCase();
        return BY_CONTENT_TYPE.get(mediaType);
    }
}
