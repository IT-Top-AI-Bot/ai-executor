package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.ittopaiexecutor.dto.SolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.producer.HomeworkResultProducer;
import com.aquadev.ittopaiexecutor.service.file.DownloadedFile;
import com.aquadev.ittopaiexecutor.service.file.FileDownloader;
import com.aquadev.ittopaiexecutor.service.file.FileGenerationService;
import com.aquadev.ittopaiexecutor.service.file.S3UploadService;
import com.aquadev.ittopaiexecutor.service.homework.HomeworkSolver;
import com.aquadev.ittopaiexecutor.service.subject.SubjectSyncService;
import com.aquadev.ittopaiexecutor.util.ContentTypeUtils;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExecutionHandlerImpl implements HomeworkExecutionHandler {

    private final Tracer tracer;
    private final FileDownloader fileDownloader;
    private final HomeworkSolver homeworkSolver;
    private final S3UploadService s3UploadService;
    private final FileGenerationService fileGenerationService;
    private final HomeworkResultProducer homeworkResultProducer;
    private final SubjectSyncService subjectSyncService;

    @Override
    public void handle(HomeworkExecutionEvent event) {
        log.info("Handling event: executionId={}, homeworkId={}, specId={}",
                event.id(), event.homeworkId(), event.specId());

        subjectSyncService.sync(event.specId(), event.nameSpec(), event.teacherFio());

        var span = tracer.currentSpan();
        if (span != null) {
            span.tag("homework.executionId", event.id().toString())
                    .tag("homework.homeworkId", event.homeworkId().toString());
        }

        DownloadedFile downloaded = fileDownloader.download(event.homeworkUrl(), event.id());
        log.info("Downloaded: filename={}, size={} bytes", downloaded.filename(), downloaded.content().length);

        SolveRequest solveRequest = new SolveRequest(
                downloaded.content(), downloaded.filename(), event.specId(), event.homeworkId(),
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
        String contentType = ContentTypeUtils.forExtension(solution.extension());

        String uploadedKey = s3UploadService.upload(fileBytes, s3Key, contentType);
        log.info("Uploaded to S3: key={}", uploadedKey);

        homeworkResultProducer.sendCompleted(event.id(), uploadedKey);
        log.info("Result sent for executionId={}", event.id());
    }

    private String buildS3Key(HomeworkExecutionEvent event, String filename) {
        return "%d-%s-%s".formatted(event.homeworkId(), event.id(), filename);
    }
}
