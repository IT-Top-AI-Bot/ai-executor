package com.aquadev.aiexecutor.service.file;

import com.aquadev.aiexecutor.util.ContentTypeRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class FilenameResolver {

    private final ContentTypeRegistry contentTypeRegistry;

    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\s*=\\s*\"?([^\"\\s;]+)\"?", Pattern.CASE_INSENSITIVE);

    public String resolve(String url, @Nullable String contentDisposition, @Nullable String contentType) {
        if (contentDisposition != null) {
            Matcher m = FILENAME_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                String name = m.group(1);
                if (!FilenameUtils.getExtension(name).isBlank()) {
                    return name;
                }
            }
        }

        try {
            String path = new URI(url).getPath();
            String name = FilenameUtils.getName(path);
            if (!name.isBlank() && !FilenameUtils.getExtension(name).isBlank()) {
                return name;
            }
        } catch (Exception _) {
            log.warn("Could not parse URL path for filename extraction: {}", url);
        }

        String ext = contentTypeRegistry.extensionForContentType(contentType);
        if (ext != null) {
            log.debug("Resolved filename extension from Content-Type={}: ext={}", contentType, ext);
            return "homework." + ext;
        }

        log.warn("Could not determine filename from URL, headers or Content-Type: url={}", url);
        return "homework";
    }
}
