package com.aquadev.aiexecutor.service.file;

import com.aquadev.aiexecutor.config.client.FileDownloadProperties;
import com.aquadev.aiexecutor.dto.DownloadedFile;
import com.aquadev.aiexecutor.util.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {

    private static final Set<String> KNOWN_EXTENSIONS = Set.of(
            // Documents
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv",
            "txt", "epub", "rtf", "odt", "xml", "bib", "fb2", "ipynb", "tex", "opml",
            // Images
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "tiff", "tif", "avif", "heic", "heif"
    );

    private final DownloadUrlValidator urlValidator;
    private final FilenameResolver filenameResolver;
    private final FileTypeDetector fileTypeDetector;
    private final FileDownloadProperties properties;
    private final RestClient fileDownloaderRestClient;

    @Override
    public DownloadedFile download(String url, UUID executionId) {
        URI uri = urlValidator.validate(url);
        log.info("Downloading homework: host={}, executionId={}", uri.getHost(), executionId);
        return fileDownloaderRestClient.get()
                .uri(uri)
                .exchange((_, response) -> processResponse(uri, response, executionId));
    }

    private DownloadedFile processResponse(URI uri, ClientHttpResponse response, UUID executionId) throws IOException {
        validateResponseStatus(response.getStatusCode().value(), uri);

        long contentLength = response.getHeaders().getContentLength();
        if (contentLength > properties.getMaxFileSizeBytes()) {
            throw new IllegalArgumentException(
                    "File exceeds max size: %d bytes > %d bytes".formatted(contentLength, properties.getMaxFileSizeBytes()));
        }

        byte[] content;
        try {
            content = readWithLimit(response.getBody(), properties.getMaxFileSizeBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read response body from: " + uri, e);
        }

        String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        MediaType contentType = response.getHeaders().getContentType();
        String filename = filenameResolver.resolve(uri.toString(), contentDisposition,
                contentType != null ? contentType.toString() : null);

        filename = normalizeFilenameExtension(filename, content, executionId);

        log.info("Downloaded {} bytes, filename={}, executionId={}", content.length, filename, executionId);

        return new DownloadedFile(content, filename);
    }

    private void validateResponseStatus(int status, URI uri) {
        if (status >= 400 && status < 500) {
            throw new IllegalArgumentException("Download rejected with status=%d for %s".formatted(status, uri));
        }
        if (status >= 500) {
            throw new IllegalStateException("Download failed with retryable status=%d for %s".formatted(status, uri));
        }
    }

    private String normalizeFilenameExtension(String filename, byte[] content, UUID executionId) {
        String currentExt = FilenameUtils.getExtension(filename).toLowerCase();
        if (filename.contains(".") && KNOWN_EXTENSIONS.contains(currentExt)) {
            return filename;
        }
        String detectedExt = fileTypeDetector.detectExtension(content);
        String baseName = filename.contains(".") ? FilenameUtils.getBaseName(filename) : "homework";
        log.info("Detected file type via Tika: originalExt={}, detectedExt={}, executionId={}",
                currentExt.isEmpty() ? "(none)" : currentExt, detectedExt, executionId);
        return baseName + "." + detectedExt;
    }

    private byte[] readWithLimit(@Nullable InputStream body, long maxBytes) throws IOException {
        if (body == null) {
            throw new IOException("Response body is empty");
        }
        byte[] buffer = new byte[8192];
        long total = 0;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            int read;
            while ((read = body.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) {
                    throw new IllegalArgumentException(
                            "File exceeds max size while reading: %d bytes > %d bytes".formatted(total, maxBytes));
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }
}
