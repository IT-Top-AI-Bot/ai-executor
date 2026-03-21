package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.util.ContentTypeUtils;
import io.micrometer.tracing.annotation.NewSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3UploadServiceImpl implements S3UploadService {

    private final S3Client s3Client;

    @Value("${s3.bucket}")
    private String bucket;

    @Override
    public String upload(Path file, String s3Key) {
        String contentType = ContentTypeUtils.forFilename(file.getFileName().toString());
        log.info("Uploading file to S3: path={}, key={}, contentType={}", file.toAbsolutePath(), s3Key, contentType);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                file
        );

        return logAndReturn(s3Key);
    }

    @Override
    @NewSpan("homework.s3-upload")
    public String upload(byte[] content, String s3Key, String contentType) {
        log.info("Uploading bytes to S3: key={}, size={} bytes, contentType={}", s3Key, content.length, contentType);

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(s3Key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content)
        );

        return logAndReturn(s3Key);
    }

    private String logAndReturn(String s3Key) {
        log.info("Uploaded to S3: bucket={}, key={}", bucket, s3Key);
        return s3Key;
    }
}
