package com.aquadev.ittopaiexecutor.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mistralai.ocr.MistralOcrApi;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRModel;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRRequest.DocumentURLChunk;
import org.springframework.ai.mistralai.ocr.MistralOcrApi.OCRResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.stream.Collectors;

/**
 * Клиент к Mistral OCR API через встроенный Spring AI {@link MistralOcrApi}.
 * Обрабатывает PDF целиком: текст, таблицы (markdown), изображения.
 */
@Slf4j
@Service
public class MistralOcrService {

    private final MistralOcrApi mistralOcrApi;

    public MistralOcrService(
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.mistralai.api-key}") String apiKey) {

        this.mistralOcrApi = new MistralOcrApi(
                "https://api.mistral.ai", apiKey, restClientBuilder);
    }

    /**
     * Отправляет PDF-байты в Mistral OCR и возвращает Markdown-текст всех страниц.
     */
    public String extractText(byte[] pdfBytes) {
        String dataUrl = "data:application/pdf;base64," + Base64.getEncoder().encodeToString(pdfBytes);

        OCRRequest request = new OCRRequest(
                OCRModel.MISTRAL_OCR_LATEST.getValue(),
                null,                          // id — не обязателен
                new DocumentURLChunk(dataUrl), // PDF как data-URI
                null,                          // pages — все страницы
                null,                          // includeImageBase64
                null,                          // imageLimit
                null                           // imageMinSize
        );

        log.info("Calling Mistral OCR API: model={}, pdfSize={} bytes",
                OCRModel.MISTRAL_OCR_LATEST.getValue(), pdfBytes.length);

        OCRResponse response = mistralOcrApi.ocr(request).getBody();

        if (response == null || response.pages() == null || response.pages().isEmpty()) {
            throw new RuntimeException("Mistral OCR returned empty response");
        }

        String text = response.pages().stream()
                .map(MistralOcrApi.OCRPage::markdown)
                .collect(Collectors.joining("\n\n"));

        log.info("Mistral OCR completed: pages={}, totalLength={}",
                response.pages().size(), text.length());
        return text;
    }
}
