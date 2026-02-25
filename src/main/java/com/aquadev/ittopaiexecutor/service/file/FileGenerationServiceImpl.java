package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class FileGenerationServiceImpl implements FileGenerationService {

    @Override
    public byte[] generateFile(SolvedHomework solved) {
        byte[] bytes;
        if ("docx".equalsIgnoreCase(solved.extension())) {
            bytes = generateDocx(solved.content());
        } else {
            bytes = solved.content().getBytes(StandardCharsets.UTF_8);
        }
        log.info("Generated {} bytes for {}.{}", bytes.length, solved.filename(), solved.extension());
        return bytes;
    }

    private byte[] generateDocx(String content) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

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

            doc.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate DOCX content", e);
        }
    }
}
