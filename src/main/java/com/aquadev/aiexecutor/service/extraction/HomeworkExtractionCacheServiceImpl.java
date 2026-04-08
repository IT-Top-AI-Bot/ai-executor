package com.aquadev.aiexecutor.service.extraction;

import com.aquadev.aiexecutor.dto.ExtractedDocument;
import com.aquadev.aiexecutor.model.HomeworkExtraction;
import com.aquadev.aiexecutor.repository.HomeworkExtractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExtractionCacheServiceImpl implements HomeworkExtractionCacheService {

    private final HomeworkExtractionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtractedDocument> findCached(Long homeworkId, String documentHash) {
        return repository.findByHomeworkId(homeworkId)
                .filter(e -> documentHash.equals(e.getDocumentHash()))
                .map(e -> {
                    log.debug("Cache hit for homeworkId={}", homeworkId);
                    return new ExtractedDocument(e.getExtractedText(), Map.of());
                });
    }

    @Override
    @Transactional
    public void saveOrUpdate(Long homeworkId, String documentHash, ExtractedDocument doc) {
        HomeworkExtraction extraction = repository.findByHomeworkId(homeworkId)
                .orElseGet(HomeworkExtraction::new);
        extraction.setHomeworkId(homeworkId);
        extraction.setExtractedText(stripNullBytes(doc.text()));
        extraction.setDocumentHash(documentHash);
        repository.save(extraction);
        log.debug("Cached extraction for homeworkId={}", homeworkId);
    }

    private static String stripNullBytes(String text) {
        return text == null ? null : text.replace("\u0000", "");
    }
}
