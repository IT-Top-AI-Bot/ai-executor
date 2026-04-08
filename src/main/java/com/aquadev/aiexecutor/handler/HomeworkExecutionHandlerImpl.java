package com.aquadev.aiexecutor.handler;

import com.aquadev.aiexecutor.dto.DownloadedFile;
import com.aquadev.aiexecutor.dto.SolveRequest;
import com.aquadev.aiexecutor.dto.SolvedHomework;
import com.aquadev.aiexecutor.producer.HomeworkResultProducer;
import com.aquadev.aiexecutor.service.file.FileDownloader;
import com.aquadev.aiexecutor.service.file.FileGenerationService;
import com.aquadev.aiexecutor.service.file.S3UploadService;
import com.aquadev.aiexecutor.service.homework.HomeworkSolver;
import com.aquadev.aiexecutor.service.subject.SubjectSyncService;
import com.aquadev.aiexecutor.util.ContentTypeRegistry;
import com.aquadev.commonlibs.HomeworkExecutionEvent;
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
    private final SubjectSyncService subjectSyncService;
    private final ContentTypeRegistry contentTypeRegistry;

    @Override
    public void handle(HomeworkExecutionEvent event) {
        log.info("Handling event: executionId={}, homeworkId={}, specId={}",
                event.id(), event.homeworkId(), event.specId());
        try {
            subjectSyncService.sync(event.specId(), event.nameSpec(), event.teacherFio());

            DownloadedFile downloaded = fileDownloader.download(event.homeworkUrl(), event.id());
            log.info("Downloaded: filename={}, size={} bytes", downloaded.filename(), downloaded.content().length);

            SolveRequest solveRequest = new SolveRequest(
                    downloaded.content(), downloaded.filename(), event.specId(), event.homeworkId(),
                    event.telegramUserId(),
                    event.theme(), event.teacherFio(), event.nameSpec(), event.comment());

            SolvedHomework solution = homeworkSolver.solve(solveRequest);
            log.info("Solution generated: filename={}, extension={}", solution.filename(), solution.extension());

            if (solution.extension() == null) {
                homeworkResultProducer.sendCompletedText(event.id(), solution.content());
                log.info("Text result sent for executionId={}", event.id());
                return;
            }

            byte[] fileBytes = fileGenerationService.generateFile(solution);

            String outputFilename = solution.filename() + "." + solution.extension();
            String s3Key = buildS3Key(event, outputFilename);
            String contentType = contentTypeRegistry.forExtension(solution.extension());

            String uploadedKey = s3UploadService.upload(fileBytes, s3Key, contentType);
            log.info("Uploaded to S3: key={}", uploadedKey);

            homeworkResultProducer.sendCompleted(event.id(), uploadedKey);
            log.info("Result sent for executionId={}", event.id());

        } catch (Exception ex) {
            log.error("Failed to handle execution: executionId={}", event.id(), ex);
            String errorMessage = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            homeworkResultProducer.sendFailed(event.id(), errorMessage);
        }
    }

    private String buildS3Key(HomeworkExecutionEvent event, String filename) {
        return "%d-%s-%s".formatted(event.homeworkId(), event.id(), filename);
    }
}
