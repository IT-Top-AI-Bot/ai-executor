package com.aquadev.ittopaiexecutor.service.file;

import java.nio.file.Path;
import java.util.UUID;

public interface FileDownloader {

    Path downloadToTempFile(String url, UUID executionId);
}
