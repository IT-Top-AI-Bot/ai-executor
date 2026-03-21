package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.exception.domain.OcrFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.content.Media;
import org.springframework.ai.mistralai.ocr.MistralOcrApi;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRModel;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest.DocumentURLChunk;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class MistralOcrService {

    private final MistralOcrApi mistralOcrApi;

    public record OcrResult(String text, List<Media> images) {
    }

    @SuppressWarnings("DataFlowIssue")
    public OcrResult extract(byte[] pdfBytes) {
        String dataUrl = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);

        OCRRequest request = new OCRRequest(
                OCRModel.MISTRAL_OCR_LATEST.getValue(),
                null,                        // id — optional
                new DocumentURLChunk(dataUrl),
                null,                        // pages — process all pages
                true,                        // includeImageBase64
                null,                        // imageLimit — no limit
                null                         // imageMinSize — no min size
        );

        log.info("Calling Mistral OCR API: model={}, pdfSize={} bytes",
                OCRModel.MISTRAL_OCR_LATEST.getValue(), pdfBytes.length);

        OCRResponse response = mistralOcrApi.ocr(request).getBody();

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

        log.info("Mistral OCR completed: pages={}, totalLength={}, images={}",
                response.pages().size(), text.length(), images.size());

        return new OcrResult(text, images);
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

            return new Media(mimeType, new ByteArrayResource(bytes));
        } catch (Exception e) {
            log.warn("Failed to decode OCR image id={}: {}", img.id(), e.getMessage());
            return null;
        }
    }
}
