package com.aquadev.ittopaiexecutor.service.extraction;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;

import java.util.Optional;

public interface HomeworkExtractionCacheService {

    Optional<ExtractedDocument> findByHomeworkId(Long homeworkId);

    void save(Long homeworkId, ExtractedDocument doc);
}
