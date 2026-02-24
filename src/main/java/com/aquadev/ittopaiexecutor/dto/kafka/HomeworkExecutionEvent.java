package com.aquadev.ittopaiexecutor.dto.kafka;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HomeworkExecutionEvent(
        UUID id,
        String theme,
        Long specId,
        HomeworkExecutionStatus status,
        String comment,
        Long groupId,
        Long teachId,
        String nameSpec,
        Instant createdAt,
        Long homeworkId,
        String teacherFio,
        String homeworkUrl,
        LocalDate overdueTime,
        LocalDate completionTime
) {
}
