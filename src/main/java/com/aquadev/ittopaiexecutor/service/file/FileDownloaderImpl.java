package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.config.client.FileDownloadProperties;
import com.aquadev.ittopaiexecutor.util.ContentTypeUtils;
import com.aquadev.ittopaiexecutor.util.FileTypeDetector;
import io.micrometer.tracing.annotation.NewSpan;
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
import java.net.IDN;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {

    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\s*=\\s*\"?([^\"\\s;]+)\"?", Pattern.CASE_INSENSITIVE);

    private static final Set<String> KNOWN_EXTENSIONS = Set.of(
            "doc", "docx", "pptx", "ppt", "xlsx", "xls", "pdf", "txt",
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "tiff", "tif"
    );

    private final FileTypeDetector fileTypeDetector;
    private final FileDownloadProperties properties;
    private final RestClient fileDownloaderRestClient;

    @Override
    @NewSpan("file.download")
    public DownloadedFile download(String url, UUID executionId) {
        URI uri = validateDownloadUri(url);
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
        String filename = resolveFilename(uri.toString(), contentDisposition,
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

    private String resolveFilename(String url, @Nullable String contentDisposition, @Nullable String contentType) {
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

        String ext = ContentTypeUtils.extensionForContentType(contentType);
        if (ext != null) {
            log.debug("Resolved filename extension from Content-Type={}: ext={}", contentType, ext);
            return "homework." + ext;
        }

        log.warn("Could not determine filename with extension from URL, headers or Content-Type: url={}", url);
        return "homework";
    }

    private URI validateDownloadUri(String rawUrl) {
        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid homework URL: " + rawUrl, ex);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("Only HTTP/HTTPS URLs are allowed: " + rawUrl);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is missing: " + rawUrl);
        }

        String normalizedHost = normalizeHost(host);
        if (!isHostAllowed(normalizedHost, properties.getAllowedHosts())) {
            throw new IllegalArgumentException("Host is not in allowlist: " + normalizedHost);
        }

        return uri;
    }

    private String normalizeHost(String host) {
        try {
            return IDN.toASCII(host).toLowerCase(Locale.ROOT);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid URL host: " + host, ex);
        }
    }

    private boolean isHostAllowed(String host, List<String> allowedHosts) {
        return allowedHosts.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> normalizeHost(value.trim()))
                .anyMatch(allowed -> matchesHost(host, allowed));
    }

    private boolean matchesHost(String host, String allowedPattern) {
        if ("*".equals(allowedPattern)) {
            return true;
        }
        if (allowedPattern.startsWith("*.")) {
            String suffix = allowedPattern.substring(2);
            return host.endsWith("." + suffix);
        }
        return host.equals(allowedPattern);
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
