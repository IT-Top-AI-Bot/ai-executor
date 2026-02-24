package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
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

            if ("docx".equalsIgnoreCase(solved.extension())) {
                writeDocx(solved.content(), file);
            } else {
                Files.writeString(file, solved.content());
            }

            log.info("Generated file: {}", file.toAbsolutePath());
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write generated file", e);
        }
    }

    private void writeDocx(String content, Path output) throws IOException {
        try (XWPFDocument doc = new XWPFDocument();
             OutputStream os = Files.newOutputStream(output)) {

            String[] paragraphs = content.split("\n\n");
            boolean firstParagraph = true;

            for (String paragraph : paragraphs) {
                String trimmed = paragraph.strip();
                if (trimmed.isBlank()) continue;

                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();

                if (firstParagraph) {
                    p.setAlignment(ParagraphAlignment.CENTER);
                    run.setBold(true);
                    run.setFontSize(14);
                    firstParagraph = false;
                } else {
                    run.setFontSize(12);
                }

                run.setText(trimmed);
            }

            doc.write(os);
        }
    }
}
