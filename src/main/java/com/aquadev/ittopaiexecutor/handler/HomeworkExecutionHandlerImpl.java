package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionEvent;
import com.aquadev.ittopaiexecutor.producer.HomeworkResultProducer;
import com.aquadev.ittopaiexecutor.service.file.FileDownloader;
import com.aquadev.ittopaiexecutor.service.file.FileGenerationService;
import com.aquadev.ittopaiexecutor.service.file.S3UploadService;
import com.aquadev.ittopaiexecutor.service.homework.HomeworkSolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionHandlerImpl implements HomeworkExecutionHandler {

    private final FileDownloader fileDownloader;
    private final HomeworkSolver homeworkSolver;
    private final S3UploadService s3UploadService;
    private final FileGenerationService fileGenerationService;
    private final HomeworkResultProducer homeworkResultProducer;

    @Override
    public void handle(HomeworkExecutionEvent event) {
        log.info("Handling event: executionId={}, homeworkId={}, specId={}",
                event.id(), event.homeworkId(), event.specId());

        Path tempFile = null;
        Path generatedFile = null;

        try {
            tempFile = fileDownloader.downloadToTempFile(event.homeworkUrl(), event.id());
            log.info("Downloaded file to temp: {}", tempFile);

            SolvedHomework solution = homeworkSolver.solve(tempFile, event.specId());
            log.info("Solution generated: filename={}.{}", solution.filename(), solution.extension());

            generatedFile = fileGenerationService.generateFile(solution);
            log.info("Generated file: {}", generatedFile.toAbsolutePath());

            String s3Key = buildS3Key(event, generatedFile.getFileName().toString());
            log.info("Uploading to S3: key={}", s3Key);

            String uploadedKey = s3UploadService.upload(generatedFile.toAbsolutePath(), s3Key);
            log.info("Uploaded to S3: key={}", uploadedKey);

            homeworkResultProducer.sendCompleted(event.id(), uploadedKey, null, null);
            log.info("DONE event sent for executionId={}", event.id());

        } catch (Exception e) {
            log.error("Homework execution failed: executionId={}, error={}", event.id(), e.getMessage(), e);
            trySendFailed(event, e.getMessage());
            // не прокидываем исключение — consumer сделает ack, повторных попыток не будет.
            // AI-обработка детерминирована: та же ошибка повторится при retry.

        } finally {
            deleteQuietly(tempFile);
            deleteQuietly(generatedFile);
        }
    }

    private String buildS3Key(HomeworkExecutionEvent event, String filename) {
        return "%d-%s-%s".formatted(event.homeworkId(), event.id(), filename);
    }

    private void trySendFailed(HomeworkExecutionEvent event, String errorMessage) {
        try {
            homeworkResultProducer.sendFailed(event.id(), errorMessage, null, null);
            log.info("FAILED event sent for executionId={}", event.id());
        } catch (Exception ex) {
            log.error("Could not send FAILED event for executionId={}: {}", event.id(), ex.getMessage(), ex);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete temp file {}: {}", path, e.getMessage());
        }
    }
}
