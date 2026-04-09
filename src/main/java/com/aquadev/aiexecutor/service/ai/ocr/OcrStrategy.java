package com.aquadev.aiexecutor.service.ai.ocr;

import java.util.Set;

public interface OcrStrategy extends OcrService {

    Set<String> supportedMimeTypes();
}
