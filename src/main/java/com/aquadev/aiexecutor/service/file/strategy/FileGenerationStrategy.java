package com.aquadev.aiexecutor.service.file.strategy;

import java.util.Set;

public interface FileGenerationStrategy {

    /**
     * Расширения файлов, которые обрабатывает эта стратегия (в нижнем регистре).
     * Пустой Set означает fallback-стратегию.
     */
    Set<String> supportedExtensions();

    byte[] generate(String content);
}
