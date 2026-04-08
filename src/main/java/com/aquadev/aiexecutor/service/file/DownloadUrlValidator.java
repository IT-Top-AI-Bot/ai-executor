package com.aquadev.aiexecutor.service.file;

import com.aquadev.aiexecutor.config.client.FileDownloadProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.IDN;
import java.net.URI;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DownloadUrlValidator {

    private final FileDownloadProperties properties;

    public URI validate(String rawUrl) {
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
}
