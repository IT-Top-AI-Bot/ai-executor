package com.aquadev.ittopaiexecutor.util;


import org.apache.tika.mime.MimeTypes;
import org.jspecify.annotations.Nullable;

import java.util.Map;

public final class ContentTypeUtils {

    private static final Map<String, String> BY_EXTENSION = Map.ofEntries(
            Map.entry("html", "text/html; charset=utf-8"),
            Map.entry("htm", "text/html; charset=utf-8"),
            Map.entry("css", "text/css; charset=utf-8"),
            Map.entry("js", "application/javascript"),
            Map.entry("ts", "application/typescript"),
            Map.entry("py", "text/x-python; charset=utf-8"),
            Map.entry("java", "text/x-java-source; charset=utf-8"),
            Map.entry("txt", "text/plain; charset=utf-8"),
            Map.entry("md", "text/markdown; charset=utf-8"),
            Map.entry("json", "application/json"),
            Map.entry("xml", "application/xml"),
            Map.entry("csv", "text/csv; charset=utf-8"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("doc", "application/msword"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("zip", "application/zip"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg")
    );

    private static final Map<String, String> BY_CONTENT_TYPE = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/msword", "doc",
            "text/plain", "txt"
    );

    private ContentTypeUtils() {
    }


    public static String forExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return MimeTypes.OCTET_STREAM;
        }
        return BY_EXTENSION.getOrDefault(extension.toLowerCase(), MimeTypes.OCTET_STREAM);
    }

    public static String forFilename(String filename) {
        if (filename == null || !filename.contains(".")) {
            return MimeTypes.OCTET_STREAM;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        return forExtension(ext);
    }

    @Nullable
    public static String extensionForContentType(@Nullable String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        String mediaType = contentType.split(";")[0].trim().toLowerCase();
        return BY_CONTENT_TYPE.get(mediaType);
    }
}
