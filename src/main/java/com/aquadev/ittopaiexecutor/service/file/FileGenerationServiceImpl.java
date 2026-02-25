package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    // ─── DOCX generation ──────────────────────────────────────────────────────

    private byte[] generateDocx(String content) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            String[] lines = content.split("\n");
            List<String> tableBuffer = new ArrayList<>();
            boolean isFirstParagraph = true;

            for (String rawLine : lines) {
                String trimmed = rawLine.strip();

                if (isTableRow(trimmed)) {
                    tableBuffer.add(trimmed);
                } else {
                    if (!tableBuffer.isEmpty()) {
                        renderTable(doc, tableBuffer);
                        tableBuffer.clear();
                        isFirstParagraph = false;
                    }
                    if (!trimmed.isBlank()) {
                        renderLine(doc, trimmed, isFirstParagraph);
                        isFirstParagraph = false;
                    }
                }
            }

            // Flush trailing table
            if (!tableBuffer.isEmpty()) {
                renderTable(doc, tableBuffer);
            }

            doc.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate DOCX content", e);
        }
    }

    // ─── Table ────────────────────────────────────────────────────────────────

    private boolean isTableRow(String line) {
        return line.startsWith("|") && line.contains("|");
    }

    /** Separator rows look like: | --- | :--- | ---: | */
    private boolean isSeparatorRow(String line) {
        return line.replaceAll("[|:\\-\\s]", "").isEmpty();
    }

    private void renderTable(XWPFDocument doc, List<String> tableLines) {
        List<String[]> rows = new ArrayList<>();
        for (String line : tableLines) {
            if (isSeparatorRow(line)) continue;
            String[] cells = parseCells(line);
            if (cells.length > 0) rows.add(cells);
        }
        if (rows.isEmpty()) return;

        int numCols = rows.get(0).length;
        XWPFTable table = doc.createTable();

        // Build header row (first row already exists with 1 cell)
        XWPFTableRow headerRow = table.getRow(0);
        while (headerRow.getTableCells().size() < numCols) {
            headerRow.addNewTableCell();
        }
        fillRow(headerRow, rows.get(0), numCols, true);

        // Build data rows
        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = table.createRow();
            while (row.getTableCells().size() < numCols) {
                row.addNewTableCell();
            }
            fillRow(row, rows.get(i), numCols, false);
        }
    }

    private void fillRow(XWPFTableRow row, String[] cells, int numCols, boolean isHeader) {
        for (int j = 0; j < numCols; j++) {
            String text = j < cells.length ? cells[j].strip() : "";
            XWPFTableCell cell = row.getCell(j);

            XWPFParagraph para = cell.getParagraphs().get(0);
            // Remove existing runs left by POI's default cell creation
            for (int k = para.getRuns().size() - 1; k >= 0; k--) {
                para.removeRun(k);
            }
            XWPFRun run = para.createRun();
            run.setText(text);
            run.setFontSize(10);
            run.setBold(isHeader);
        }
    }

    private String[] parseCells(String line) {
        String inner = line.strip();
        if (inner.startsWith("|")) inner = inner.substring(1);
        if (inner.endsWith("|"))   inner = inner.substring(0, inner.length() - 1);
        return inner.split("\\|", -1);
    }

    // ─── Paragraphs ───────────────────────────────────────────────────────────

    private void renderLine(XWPFDocument doc, String line, boolean isFirstParagraph) {
        if (line.startsWith("### ")) {
            addParagraph(doc, line.substring(4).strip(), 13, true, ParagraphAlignment.LEFT);
        } else if (line.startsWith("## ")) {
            addParagraph(doc, line.substring(3).strip(), 14, true, ParagraphAlignment.LEFT);
        } else if (line.startsWith("# ")) {
            addParagraph(doc, line.substring(2).strip(), 16, true, ParagraphAlignment.CENTER);
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            addParagraph(doc, "• " + line.substring(2).strip(), 11, false, ParagraphAlignment.LEFT);
        } else if (line.matches("^\\d+\\.\\s.*")) {
            addParagraph(doc, line, 11, false, ParagraphAlignment.LEFT);
        } else if (isFirstParagraph) {
            // First text block = document title
            addParagraph(doc, line, 14, true, ParagraphAlignment.CENTER);
        } else {
            addParagraph(doc, line, 11, false, ParagraphAlignment.LEFT);
        }
    }

    private void addParagraph(XWPFDocument doc, String text, int fontSize,
                               boolean bold, ParagraphAlignment align) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        addInlineRuns(p, text, fontSize, bold);
    }

    /**
     * Parses inline **bold** markers and creates alternating bold/normal runs.
     */
    private void addInlineRuns(XWPFParagraph para, String text, int fontSize, boolean paragraphBold) {
        boolean inBold = false;
        int i = 0;
        StringBuilder segment = new StringBuilder();

        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                if (!segment.isEmpty()) {
                    createRun(para, segment.toString(), fontSize, paragraphBold || inBold);
                    segment.setLength(0);
                }
                inBold = !inBold;
                i += 2;
            } else {
                segment.append(text.charAt(i));
                i++;
            }
        }
        if (!segment.isEmpty()) {
            createRun(para, segment.toString(), fontSize, paragraphBold || inBold);
        }
    }

    private void createRun(XWPFParagraph para, String text, int fontSize, boolean bold) {
        XWPFRun run = para.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(bold);
    }
}
