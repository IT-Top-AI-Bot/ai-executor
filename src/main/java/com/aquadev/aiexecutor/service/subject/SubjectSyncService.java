package com.aquadev.aiexecutor.service.subject;

public interface SubjectSyncService {

    void sync(Long apiSubjectId, String name, String teacherFio);
}
