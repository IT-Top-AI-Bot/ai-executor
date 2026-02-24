package com.aquadev.ittopaiexecutor.service.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("html",  "text/html; charset=utf-8"),
            Map.entry("htm",   "text/html; charset=utf-8"),
            Map.entry("css",   "text/css; charset=utf-8"),
            Map.entry("js",    "application/javascript"),
            Map.entry("ts",    "application/typescript"),
            Map.entry("py",    "text/x-python; charset=utf-8"),
            Map.entry("java",  "text/x-java-source; charset=utf-8"),
            Map.entry("txt",   "text/plain; charset=utf-8"),
            Map.entry("md",    "text/markdown; charset=utf-8"),
            Map.entry("json",  "application/json"),
            Map.entry("xml",   "application/xml"),
            Map.entry("csv",   "text/csv; charset=utf-8"),
            Map.entry("pdf",   "application/pdf"),
            Map.entry("docx",  "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("doc",   "application/msword"),
            Map.entry("xlsx",  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("zip",   "application/zip"),
            Map.entry("png",   "image/png"),
            Map.entry("jpg",   "image/jpeg"),
            Map.entry("jpeg",  "image/jpeg")
    );

    private final S3Client s3Client;

    @Value("${s3.bucket}")
    private String bucket;

    @Override
    public String upload(Path file, String s3Key) {
        String contentType = detectContentType(file.getFileName().toString());
        log.info("Uploading file to S3: path={}, key={}, contentType={}", file.toAbsolutePath(), s3Key, contentType);

        ensureBucketExists();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                file
        );

        log.info("Uploaded to S3: bucket={}, key={}", bucket, s3Key);
        return s3Key;
    }

    @Override
    public String upload(byte[] content, String s3Key, String contentType) {
        log.info("Uploading bytes to S3: key={}, contentType={}", s3Key, contentType);

        ensureBucketExists();

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        log.info("Uploaded to S3: bucket={}, key={}", bucket, s3Key);
        return s3Key;
    }

    private void ensureBucketExists() {
        s3Client.createBucket(b -> b.bucket(bucket));
    }

    private String detectContentType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "application/octet-stream";
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
