package com.aquadev.aiexecutor.service.extraction;

import com.aquadev.aiexecutor.dto.ExtractedDocument;

import java.util.Optional;

public interface HomeworkExtractionCacheService {

    /**
     * Returns cached extraction only if the stored document hash matches {@code documentHash}.
     * Empty if not cached or the document has changed.
     */
    Optional<ExtractedDocument> findCached(Long homeworkId, String documentHash);

    /**
     * Saves or updates the cached extraction along with the document hash.
     */
    void saveOrUpdate(Long homeworkId, String documentHash, ExtractedDocument doc);
}
