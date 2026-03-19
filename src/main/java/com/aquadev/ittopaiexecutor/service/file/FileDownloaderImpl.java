package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.config.client.FileDownloadProperties;
import com.aquadev.ittopaiexecutor.util.ContentTypeUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
public class FileDownloaderImpl implements FileDownloader {

    /** RFC 6266: Content-Disposition filename parameter (quoted or unquoted). */
    private static final Pattern FILENAME_PATTERN =
            Pattern.compile("filename\\s*=\\s*\"?([^\"\\s;]+)\"?", Pattern.CASE_INSENSITIVE);

    private static final Set<String> KNOWN_EXTENSIONS = Set.of("doc", "docx", "pdf", "txt");

    private final RestClient restClient;
    private final FileDownloadProperties properties;

    public FileDownloaderImpl(
            @Qualifier("fileDownloaderRestClient") RestClient restClient,
            FileDownloadProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public DownloadedFile download(String url, UUID executionId) {
        URI uri = validateDownloadUri(url);
        log.info("Downloading homework: host={}, executionId={}", uri.getHost(), executionId);

        return restClient.get()
                .uri(uri)
                .exchange((request, response) -> {
                    int status = response.getStatusCode().value();
                    if (status >= 400 && status < 500) {
                        throw new IllegalArgumentException("Download rejected with status=%d for %s".formatted(status, uri));
                    }
                    if (status >= 500) {
                        throw new IllegalStateException("Download failed with retryable status=%d for %s".formatted(status, uri));
                    }

                    long contentLength = response.getHeaders().getContentLength();
                    if (contentLength > properties.getMaxFileSizeBytes()) {
                        throw new IllegalArgumentException(
                                "File exceeds max size: %d bytes > %d bytes".formatted(
                                        contentLength, properties.getMaxFileSizeBytes()));
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

                    String currentExt = FilenameUtils.getExtension(filename).toLowerCase();
                    if (!filename.contains(".") || !KNOWN_EXTENSIONS.contains(currentExt)) {
                        String sniffedExt = sniffExtension(content);
                        String baseName = filename.contains(".") ? FilenameUtils.getBaseName(filename) : "homework";
                        filename = baseName + "." + sniffedExt;
                        log.info("Detected file type by magic bytes: originalExt={}, sniffedExt={}, executionId={}",
                                currentExt.isEmpty() ? "(none)" : currentExt, sniffedExt, executionId);
                    }

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
     *   <li>Content-Type header — {@code application/pdf} → {@code homework.pdf}</li>
     *   <li>Fallback: {@code "homework"} (DocumentStrategyResolver will throw on unknown extension)</li>
     * </ol>
     */
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
        } catch (Exception e) {
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

    /**
     * Detects document type by magic bytes (file signature).
     * <ul>
     *   <li>PDF:  {@code %PDF} → {@code pdf}</li>
     *   <li>DOCX: {@code PK\x03\x04} (ZIP) → {@code docx}</li>
     *   <li>DOC:  {@code D0 CF 11 E0} (OLE2) → {@code doc}</li>
     *   <li>Fallback → {@code txt}</li>
     * </ul>
     */
    private String sniffExtension(byte[] content) {
        if (content.length >= 4) {
            if (content[0] == 0x25 && content[1] == 0x50 && content[2] == 0x44 && content[3] == 0x46) {
                return "pdf";
            }
            if (content[0] == 0x50 && content[1] == 0x4B && content[2] == 0x03 && content[3] == 0x04) {
                return "docx";
            }
            if (content[0] == (byte) 0xD0 && content[1] == (byte) 0xCF
                    && content[2] == 0x11 && content[3] == (byte) 0xE0) {
                return "doc";
            }
        }
        return "txt";
    }

    private byte[] readWithLimit(@Nullable java.io.InputStream body, long maxBytes) throws IOException {
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
