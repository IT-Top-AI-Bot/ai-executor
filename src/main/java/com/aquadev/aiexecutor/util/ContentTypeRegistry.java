package com.aquadev.aiexecutor.util;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ContentTypeRegistry {

    private static final String OCTET_STREAM = "application/octet-stream";

    private final Map<String, String> byExtension;
    private final Map<String, String> byContentType;

    public ContentTypeRegistry(List<ContentTypeMapping> mappings) {
        Map<String, String> ext = new HashMap<>();
        Map<String, String> ct = new HashMap<>();
        mappings.forEach(m -> {
            ext.putAll(m.extensionToContentType());
            ct.putAll(m.contentTypeToExtension());
        });
        this.byExtension = Collections.unmodifiableMap(ext);
        this.byContentType = Collections.unmodifiableMap(ct);
    }

    public String forExtension(@Nullable String extension) {
        if (extension == null || extension.isBlank()) return OCTET_STREAM;
        return byExtension.getOrDefault(extension.toLowerCase(), OCTET_STREAM);
    }

    public String forFilename(@Nullable String filename) {
        if (filename == null || !filename.contains(".")) return OCTET_STREAM;
        String ext = filename.substring(filename.lastIndexOf('.') + 1);
        return forExtension(ext);
    }

    @Nullable
    public String extensionForContentType(@Nullable String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        String mediaType = contentType.split(";")[0].trim().toLowerCase();
        return byContentType.get(mediaType);
    }
}
