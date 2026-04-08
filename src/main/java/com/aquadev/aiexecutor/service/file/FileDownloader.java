package com.aquadev.aiexecutor.service.file;

import com.aquadev.aiexecutor.dto.DownloadedFile;

import java.util.UUID;

public interface FileDownloader {

    /**
     * Downloads the file at {@code url} into memory.
     * The returned {@link DownloadedFile} contains the raw bytes and
     * the original filename with extension, resolved from the
     * Content-Disposition header or the URL path.
     */
    DownloadedFile download(String url, UUID executionId);
}
