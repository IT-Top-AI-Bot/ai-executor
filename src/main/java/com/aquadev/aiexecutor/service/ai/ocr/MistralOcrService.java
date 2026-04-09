package com.aquadev.aiexecutor.service.ai.ocr;

import com.aquadev.aiexecutor.exception.domain.OcrFailedException;
import com.aquadev.aiexecutor.util.ImageResizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.mistralai.ocr.MistralOcrApi;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRModel;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest.DocumentURLChunk;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class MistralOcrService implements OcrService {

    private static final int OCR_INPUT_MAX_DIMENSION = 1600;
    private static final int OCR_EXTRACTED_IMAGE_MAX_DIMENSION = 1024;

    private final MistralOcrApi mistralOcrApi;

    @Override
    public OcrResult extract(byte[] content, String mimeType) {
        byte[] optimizedContent = ImageResizer.resizeIfNeeded(content, mimeType, OCR_INPUT_MAX_DIMENSION);
        String finalMimeType = optimizedContent != content ? "image/jpeg" : mimeType;

        String dataUrl = "data:" + finalMimeType + ";base64," + Base64.getEncoder().encodeToString(optimizedContent);

        OCRRequest request = buildOcrRequest(dataUrl);

        log.info("Calling Mistral OCR API: model={}, mimeType={}, size={} bytes (optimized from {}), dataUrlLength={}",
                OCRModel.MISTRAL_OCR_LATEST.getValue(), finalMimeType, optimizedContent.length, content.length, dataUrl.length());

        OCRResponse response;
        try {
            response = mistralOcrApi.ocr(request).getBody();
        } catch (Exception e) {
            log.error("Mistral OCR API call failed [{}]: {}", e.getClass().getName(), e.getMessage(), e);
            throw e;
        }

        if (response == null || response.pages().isEmpty()) {
            throw new OcrFailedException("Mistral OCR returned empty response");
        }

        String text = response.pages().stream()
                .map(MistralOcrApi.OCRPage::markdown)
                .collect(Collectors.joining("\n\n"));

        List<Media> images = response.pages().stream()
                .flatMap(p -> p.images().stream())
                .filter(img -> !img.imageBase64().isBlank())
                .map(this::toMedia)
                .filter(Objects::nonNull)
                .toList();

        int totalImageBytes = images.stream()
                .mapToInt(img -> {
                    try {
                        return img.getDataAsByteArray().length;
                    } catch (Exception _) {
                        return 0;
                    }
                }).sum();
        log.info("Mistral OCR completed: pages={}, totalLength={}, images={}, totalImageBytes={}",
                response.pages().size(), text.length(), images.size(), totalImageBytes);

        return new OcrResult(text, images);
    }

    @SuppressWarnings("DataFlowIssue")
    private static OCRRequest buildOcrRequest(String dataUrl) {
        return new OCRRequest(
                OCRModel.MISTRAL_OCR_LATEST.getValue(),
                null,   // id — optional
                new DocumentURLChunk(dataUrl),
                null,   // pages — process all pages
                true,   // includeImageBase64
                null,   // imageLimit — no limit
                null    // imageMinSize — no min size
        );
    }

    private Media toMedia(MistralOcrApi.ExtractedImage img) {
        try {
            String raw = img.imageBase64();
            byte[] bytes;
            MimeType mimeType = MimeTypeUtils.IMAGE_JPEG;

            if (raw.startsWith("data:")) {
                int comma = raw.indexOf(',');
                String header = raw.substring(5, comma);
                mimeType = MimeType.valueOf(header.split(";")[0]);
                bytes = Base64.getDecoder().decode(raw.substring(comma + 1));
            } else {
                bytes = Base64.getDecoder().decode(raw);
            }

            byte[] optimizedBytes = ImageResizer.resizeIfNeeded(bytes, mimeType.toString(), OCR_EXTRACTED_IMAGE_MAX_DIMENSION);
            if (optimizedBytes != bytes) {
                mimeType = MimeTypeUtils.IMAGE_JPEG;
            }

            return new Media(mimeType, new ByteArrayResource(optimizedBytes));
        } catch (Exception e) {
            log.warn("Failed to decode OCR image id={}: {}", img.id(), e.getMessage());
            return null;
        }
    }
}
