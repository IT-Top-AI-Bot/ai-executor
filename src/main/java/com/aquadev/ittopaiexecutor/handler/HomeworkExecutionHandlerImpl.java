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

        Path tempFile = fileDownloader.downloadToTempFile(event.homeworkUrl(), event.id());
        log.info("Downloaded file to temp: {}", tempFile);

        SolvedHomework solution = homeworkSolver.solve(tempFile, event.specId());
        log.info("Solution generated: filename={}.{}", solution.filename(), solution.extension());

        Path generatedFile = fileGenerationService.generateFile(solution);
        log.info("Generated file: {}", generatedFile.toAbsolutePath());

        String filename = generatedFile.getFileName().toString();
        String s3Key = "%d-%s-%s".formatted(
                event.homeworkId(), event.id(), filename);
        log.info("Uploading to S3: key={}", s3Key);

        String uploadedKey;
        try {
            uploadedKey = s3UploadService.upload(generatedFile.toAbsolutePath(), s3Key);
        } catch (Exception e) {
            log.error("S3 upload failed for executionId={}: {}", event.id(), e.getMessage(), e);
            throw e;
        }
        log.info("Uploaded to S3: key={}", uploadedKey);

        try {
            homeworkResultProducer.sendCompleted(event.id(), uploadedKey, null, null);
        } catch (Exception e) {
            log.error("Kafka result send failed for executionId={}: {}", event.id(), e.getMessage(), e);
            throw e;
        }
        log.info("Result event sent for executionId={}", event.id());
    }
}
