package com.aquadev.ittopaiexecutor.service.file;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileDownloaderImpl implements FileDownloader {

    private final RestClient restClient;

    @Override
    public Path downloadToTempFile(String url, UUID executionId) {
        Path temp = createTempFile(executionId, "input");

        restClient.get()
                .uri(url)
                .exchange((_, response) -> {
                    try (InputStream in = response.getBody();
                         OutputStream out = Files.newOutputStream(temp)) {
                        in.transferTo(out);
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to stream file from: " + url, e);
                    }
                    return null;
                });

        return temp;
    }

    private Path createTempFile(UUID executionId, String suffix) {
        try {
            return Files.createTempFile("hw-" + executionId + "-", "-" + suffix);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
