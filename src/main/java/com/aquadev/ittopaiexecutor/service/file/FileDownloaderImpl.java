package com.aquadev.ittopaiexecutor.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {

    /** RFC 6266: Content-Disposition filename parameter (quoted or unquoted). */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\s*=\\s*\"?([^\"\\s;]+)\"?", Pattern.CASE_INSENSITIVE);

    private final RestClient restClient;

    @Override
    public DownloadedFile download(String url, UUID executionId) {
        log.info("Downloading homework: url={}, executionId={}", url, executionId);

        return restClient.get()
                .uri(url)
                .exchange((request, response) -> {
                    byte[] content;
                    try {
                        content = response.getBody().readAllBytes();
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read response body from: " + url, e);
                    }

                    String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
                    String filename = resolveFilename(url, contentDisposition);

                    log.info("Downloaded {} bytes, filename={}, executionId={}",
                            content.length, filename, executionId);
                    return new DownloadedFile(content, filename);
                });
    }

    /**
     * Resolves the original filename by checking (in priority order):
     * <ol>
     *   <li>Content-Disposition header — {@code filename="task.pdf"}</li>
     *   <li>Last path segment of the URL — {@code /uploads/task.pdf}</li>
     *   <li>Fallback: {@code "homework"} (DocumentStrategyResolver will throw on unknown extension)</li>
     * </ol>
     */
    private String resolveFilename(String url, @Nullable String contentDisposition) {
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
        } catch (Exception e) {
            log.warn("Could not parse URL path for filename extraction: {}", url);
        }

        log.warn("Could not determine filename with extension from URL or headers: url={}", url);
        return "homework";
    }
}
