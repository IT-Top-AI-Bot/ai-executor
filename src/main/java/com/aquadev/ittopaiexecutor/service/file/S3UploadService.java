package com.aquadev.ittopaiexecutor.service.file;

import java.nio.file.Path;

public interface S3UploadService {

    String upload(Path file, String s3Key);

    String upload(byte[] content, String s3Key, String contentType);
}
