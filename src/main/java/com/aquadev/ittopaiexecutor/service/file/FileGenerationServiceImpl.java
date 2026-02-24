package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class FileGenerationServiceImpl implements FileGenerationService {

    private static final Path OUTPUT_DIR = Path.of("generated-files");

    @Override
    public Path generateFile(SolvedHomework solved) {
        try {
            Files.createDirectories(OUTPUT_DIR);
            Path file = OUTPUT_DIR.resolve(solved.filename() + "." + solved.extension());
            Files.writeString(file, solved.content());
            log.info("Generated file: {}", file.toAbsolutePath());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write generated file", e);
        }
    }
}
