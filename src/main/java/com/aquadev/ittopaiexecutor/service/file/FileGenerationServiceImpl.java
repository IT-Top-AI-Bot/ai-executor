package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.service.file.strategy.FileGenerationStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FileGenerationServiceImpl implements FileGenerationService {

    private final Map<String, FileGenerationStrategy> registry;
    private final FileGenerationStrategy fallback;

    public FileGenerationServiceImpl(List<FileGenerationStrategy> strategies) {
        this.fallback = strategies.stream()
                .filter(s -> s.supportedExtensions().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No fallback FileGenerationStrategy found"));
        this.registry = new HashMap<>();
        strategies.stream()
                .filter(s -> !s.supportedExtensions().isEmpty())
                .forEach(s -> s.supportedExtensions().forEach(ext -> registry.put(ext, s)));
    }

    @Override
    public byte[] generateFile(SolvedHomework solved) {
        FileGenerationStrategy strategy = registry.getOrDefault(
                solved.extension().toLowerCase(), fallback);
        byte[] bytes = strategy.generate(solved.content());
        log.info("Generated {} bytes for {}.{}", bytes.length, solved.filename(), solved.extension());
        return bytes;
    }
}
