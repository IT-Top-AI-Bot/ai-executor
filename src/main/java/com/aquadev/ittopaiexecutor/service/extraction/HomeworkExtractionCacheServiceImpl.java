package com.aquadev.ittopaiexecutor.service.extraction;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.entity.HomeworkExtraction;
import com.aquadev.ittopaiexecutor.entity.HomeworkExtractionImage;
import com.aquadev.ittopaiexecutor.repository.HomeworkExtractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkExtractionCacheServiceImpl implements HomeworkExtractionCacheService {

    private final HomeworkExtractionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ExtractedDocument> findByHomeworkId(Long homeworkId) {
        return repository.findByHomeworkId(homeworkId)
                .map(this::toExtractedDocument);
    }

    @Override
    @Transactional
    public void save(Long homeworkId, ExtractedDocument doc) {
        HomeworkExtraction extraction = new HomeworkExtraction();
        extraction.setHomeworkId(homeworkId);
        extraction.setExtractedText(doc.text());

        for (Media media : doc.images()) {
            HomeworkExtractionImage image = new HomeworkExtractionImage();
            image.setExtraction(extraction);
            image.setMimeType(media.getMimeType().toString());
            image.setImageData(readBytes(media));
            extraction.getImages().add(image);
        }

        repository.save(extraction);
        log.debug("Cached extraction for homeworkId={}, images={}", homeworkId, extraction.getImages().size());
    }

    private ExtractedDocument toExtractedDocument(HomeworkExtraction extraction) {
        List<Media> images = extraction.getImages().stream()
                .map(img -> new Media(
                        MimeType.valueOf(img.getMimeType()),
                        new ByteArrayResource(img.getImageData())))
                .toList();
        return new ExtractedDocument(extraction.getExtractedText(), java.util.Map.of(), images);
    }

    private byte[] readBytes(Media media) {
        return media.getDataAsByteArray();
    }
}
