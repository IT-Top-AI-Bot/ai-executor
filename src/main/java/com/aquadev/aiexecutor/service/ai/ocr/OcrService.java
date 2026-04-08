package com.aquadev.aiexecutor.service.ai.ocr;

import org.springframework.ai.content.Media;

import java.util.List;

public interface OcrService {

    record OcrResult(String text, List<Media> images) {
    }

    OcrResult extract(byte[] content, String mimeType);
}
