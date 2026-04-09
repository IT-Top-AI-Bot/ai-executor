package com.aquadev.aiexecutor.service.ai.ocr;

import com.aquadev.aiexecutor.exception.domain.OcrFailedException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component
public class OcrStrategyRouter implements OcrService {

    private final List<OcrStrategy> strategies;
    private Map<String, OcrStrategy> registry;

    public OcrStrategyRouter(List<OcrStrategy> strategies) {
        this.strategies = strategies;
    }

    @PostConstruct
    void init() {
        registry = new HashMap<>();
        strategies.forEach(s -> s.supportedMimeTypes().forEach(m -> registry.put(m, s)));
        log.info("OCR strategy registry initialized: {}", registry.keySet());
    }

    @Override
    public OcrResult extract(byte[] content, String mimeType) {
        OcrStrategy strategy = registry.get(mimeType);
        if (strategy == null) {
            throw new OcrFailedException("No OCR strategy registered for mimeType=" + mimeType);
        }
        log.info("Routing OCR: mimeType={} → {}", mimeType, strategy.getClass().getSimpleName());
        return strategy.extract(content, mimeType);
    }
}
