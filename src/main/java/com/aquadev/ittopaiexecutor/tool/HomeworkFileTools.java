package com.aquadev.ittopaiexecutor.tool;

import com.aquadev.ittopaiexecutor.dto.ToolResult;
import com.aquadev.ittopaiexecutor.service.file.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class HomeworkFileTools {

    private final S3UploadService s3UploadService;

    @Tool(description = """
            Create a .docx Word document with the homework solution.
            Use for: essays, reports, literature analysis, history, biology,
            any text-heavy academic assignments that need formatted document output.
            """)
    public ToolResult createDocxFile(
            @ToolParam(description = "Full homework solution text, use double newlines between paragraphs")
            String content,
            @ToolParam(description = "Document title / homework topic")
            String title
    ) {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph titleParagraph = doc.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(16);
            titleRun.addBreak();

            for (String paragraph : content.split("\n\n")) {
                XWPFParagraph p = doc.createParagraph();
                XWPFRun run = p.createRun();
                run.setText(paragraph.trim());
                run.setFontSize(12);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.write(out);

            String fileName = sanitizeFileName(title) + ".docx";
            String s3Key = s3UploadService.upload(out.toByteArray(), fileName,
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

            return new ToolResult(true, fileName, s3Key, "DOCX", null);
        } catch (IOException e) {
            return new ToolResult(false, null, null, "DOCX", "Failed: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9а-яА-Я_\\-]", "_").toLowerCase();
    }
}

