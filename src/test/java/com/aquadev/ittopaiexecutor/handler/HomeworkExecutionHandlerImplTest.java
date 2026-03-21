package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionStatus;
import com.aquadev.ittopaiexecutor.dto.SolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.producer.HomeworkResultProducer;
import com.aquadev.ittopaiexecutor.service.file.DownloadedFile;
import com.aquadev.ittopaiexecutor.service.file.FileDownloader;
import com.aquadev.ittopaiexecutor.service.file.FileGenerationService;
import com.aquadev.ittopaiexecutor.service.file.S3UploadService;
import com.aquadev.ittopaiexecutor.service.homework.HomeworkSolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkExecutionHandlerImplTest {

    @Mock
    FileDownloader fileDownloader;
    @Mock
    HomeworkSolver homeworkSolver;
    @Mock
    S3UploadService s3UploadService;
    @Mock
    FileGenerationService fileGenerationService;
    @Mock
    HomeworkResultProducer homeworkResultProducer;

    @InjectMocks
    HomeworkExecutionHandlerImpl handler;

    // ── handle: happy path ────────────────────────────────────────────────────

    @Test
    void handle_success_downloadsExtractsUploadsAndSendsResult() {
        HomeworkExecutionEvent event = buildEvent(42L);
        DownloadedFile downloaded = new DownloadedFile("content".getBytes(), "hw.pdf");
        SolvedHomework solution = new SolvedHomework("answer", "docx", "Answer text");
        byte[] fileBytes = "docx content".getBytes();
        String s3Key = "42-" + event.id() + "-answer.docx";

        when(fileDownloader.download(event.homeworkUrl(), event.id())).thenReturn(downloaded);
        when(homeworkSolver.solve(any(SolveRequest.class))).thenReturn(solution);
        when(fileGenerationService.generateFile(solution)).thenReturn(fileBytes);
        when(s3UploadService.upload(fileBytes,
                "42-" + event.id() + "-answer.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .thenReturn(s3Key);

        handler.handle(event);

        verify(homeworkResultProducer).sendCompleted(event.id(), s3Key);
    }

    // ── handle: S3 key format ─────────────────────────────────────────────────

    @Test
    void handle_s3KeyFormat_isHomeworkIdDashExecutionIdDashFilename() {
        UUID executionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        HomeworkExecutionEvent event = buildEvent(executionId, 99L, "http://url");

        DownloadedFile downloaded = new DownloadedFile("c".getBytes(), "task.txt");
        SolvedHomework solution = new SolvedHomework("result", "txt", "text");

        when(fileDownloader.download(any(), eq(executionId))).thenReturn(downloaded);
        when(homeworkSolver.solve(any(SolveRequest.class))).thenReturn(solution);
        when(fileGenerationService.generateFile(solution)).thenReturn("bytes".getBytes());
        when(s3UploadService.upload(any(), anyString(), anyString())).thenReturn("uploaded-key");

        handler.handle(event);

        // S3 key = "{homeworkId}-{executionId}-{filename}.{ext}"
        verify(s3UploadService).upload(any(),
                eq("99-" + executionId + "-result.txt"),
                any());
    }

    // ── handle: download failure propagates ──────────────────────────────────

    @Test
    void handle_downloadFails_exceptionPropagates() {
        HomeworkExecutionEvent event = buildEvent(10L);
        when(fileDownloader.download(any(), any()))
                .thenThrow(new RuntimeException("Network error"));

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Network error");

        verifyNoInteractions(homeworkSolver, s3UploadService, homeworkResultProducer);
    }

    // ── handle: solve failure propagates ─────────────────────────────────────

    @Test
    void handle_solveFails_exceptionPropagates() {
        HomeworkExecutionEvent event = buildEvent(10L);
        when(fileDownloader.download(any(), any()))
                .thenReturn(new DownloadedFile("c".getBytes(), "hw.pdf"));
        when(homeworkSolver.solve(any(SolveRequest.class)))
                .thenThrow(new RuntimeException("AI error"));

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI error");

        verifyNoInteractions(s3UploadService, homeworkResultProducer);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private HomeworkExecutionEvent buildEvent(long homeworkId) {
        return buildEvent(UUID.randomUUID(), homeworkId, "http://example.com/hw.pdf");
    }

    private HomeworkExecutionEvent buildEvent(UUID id, long homeworkId, String url) {
        return new HomeworkExecutionEvent(
                id, "Math homework", 5L, HomeworkExecutionStatus.PENDING,
                "Comment", 10L, 3L, "Mathematics",
                java.time.Instant.now(), homeworkId, "Teacher",
                url, LocalDate.now(), LocalDate.now()
        );
    }
}
