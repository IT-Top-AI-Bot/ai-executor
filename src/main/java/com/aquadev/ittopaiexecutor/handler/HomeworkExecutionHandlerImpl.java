package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionEvent;
import com.aquadev.ittopaiexecutor.producer.HomeworkResultProducer;
import com.aquadev.ittopaiexecutor.service.file.DownloadedFile;
import com.aquadev.ittopaiexecutor.service.file.FileDownloader;
import com.aquadev.ittopaiexecutor.service.file.FileGenerationService;
import com.aquadev.ittopaiexecutor.service.file.S3UploadService;
import com.aquadev.ittopaiexecutor.service.homework.HomeworkSolver;
import com.aquadev.ittopaiexecutor.util.ContentTypeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        try {
            DownloadedFile downloaded = fileDownloader.download(event.homeworkUrl(), event.id());
            log.info("Downloaded: filename={}, size={} bytes", downloaded.filename(), downloaded.content().length);

            SolvedHomework solution = homeworkSolver.solve(
                    downloaded.content(), downloaded.filename(), event.specId());
            log.info("Solution generated: filename={}.{}", solution.filename(), solution.extension());

            byte[] fileBytes = fileGenerationService.generateFile(solution);

            String outputFilename = solution.filename() + "." + solution.extension();
            String s3Key = buildS3Key(event, outputFilename);
            String contentType = ContentTypeUtils.forExtension(solution.extension());

            String uploadedKey = s3UploadService.upload(fileBytes, s3Key, contentType);
            log.info("Uploaded to S3: key={}", uploadedKey);

            homeworkResultProducer.sendCompleted(event.id(), uploadedKey, null, null);
            log.info("DONE event sent for executionId={}", event.id());

        } catch (Exception e) {
            log.error("Homework execution failed: executionId={}, error={}", event.id(), e.getMessage(), e);
            trySendFailed(event, e.getMessage());
            // не прокидываем — consumer вызовет ack(), повторных попыток не будет
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
}
