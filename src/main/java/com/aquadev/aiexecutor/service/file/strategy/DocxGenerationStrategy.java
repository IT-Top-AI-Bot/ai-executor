package com.aquadev.aiexecutor.service.file.strategy;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class DocxGenerationStrategy implements FileGenerationStrategy {

    private static final String FONT = "Times New Roman";
    private static final int FIRST_LINE_INDENT = 720;
    private static final int LINE_SPACING_15 = 360;

    @Override
    public Set<String> supportedExtensions() {
        return Set.of("docx");
    }

    @Override
    public byte[] generate(String content) {
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

            if (!tableBuffer.isEmpty()) {
                renderTable(doc, tableBuffer);
            }

            doc.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate DOCX content", e);
        }
    }

    private boolean isTableRow(String line) {
        return line.startsWith("|") && line.contains("|");
    }

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

        int numCols = rows.getFirst().length;
        XWPFTable table = doc.createTable();

        XWPFTableRow headerRow = table.getRow(0);
        while (headerRow.getTableCells().size() < numCols) {
            headerRow.addNewTableCell();
        }
        fillRow(headerRow, rows.getFirst(), numCols, true);

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

            XWPFParagraph para = cell.getParagraphs().getFirst();
            for (int k = para.getRuns().size() - 1; k >= 0; k--) {
                para.removeRun(k);
            }
            XWPFRun run = para.createRun();
            run.setText(text);
            run.setFontFamily(FONT);
            run.setFontSize(11);
            run.setBold(isHeader);
        }
    }

    private String[] parseCells(String line) {
        String inner = line.strip();
        if (inner.startsWith("|")) inner = inner.substring(1);
        if (inner.endsWith("|")) inner = inner.substring(0, inner.length() - 1);
        return inner.split("\\|", -1);
    }

    private void renderLine(XWPFDocument doc, String line, boolean isFirstParagraph) {
        if (line.startsWith("### ")) {
            addParagraph(doc, line.substring(4).strip(), 13, true, ParagraphAlignment.LEFT, false);
        } else if (line.startsWith("## ")) {
            addParagraph(doc, line.substring(3).strip(), 14, true, ParagraphAlignment.LEFT, false);
        } else if (line.startsWith("# ")) {
            addParagraph(doc, line.substring(2).strip(), 16, true, ParagraphAlignment.CENTER, false);
        } else if (line.startsWith("- ") || line.startsWith("* ")) {
            addParagraph(doc, "• " + line.substring(2).strip(), 12, false, ParagraphAlignment.LEFT, false);
        } else if (line.matches("^\\d+\\.\\s.*")) {
            addParagraph(doc, line, 12, false, ParagraphAlignment.LEFT, false);
        } else if (isFirstParagraph) {
            addParagraph(doc, line, 14, true, ParagraphAlignment.CENTER, false);
        } else {
            addParagraph(doc, line, 12, false, ParagraphAlignment.LEFT, true);
        }
    }

    private void addParagraph(XWPFDocument doc, String text, int fontSize,
                              boolean bold, ParagraphAlignment align, boolean indent) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(align);
        applySpacing(p, indent);
        addInlineRuns(p, text, fontSize, bold);
    }

    private void applySpacing(XWPFParagraph p, boolean firstLineIndent) {
        var pPr = p.getCTP().isSetPPr() ? p.getCTP().getPPr() : p.getCTP().addNewPPr();

        var spacing = pPr.isSetSpacing() ? pPr.getSpacing() : pPr.addNewSpacing();
        spacing.setLine(BigInteger.valueOf(LINE_SPACING_15));
        spacing.setLineRule(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STLineSpacingRule.AUTO
        );

        if (firstLineIndent) {
            var ind = pPr.isSetInd() ? pPr.getInd() : pPr.addNewInd();
            ind.setFirstLine(BigInteger.valueOf(FIRST_LINE_INDENT));
        }
    }

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
        run.setFontFamily(FONT);
        run.setFontSize(fontSize);
        run.setBold(bold);
    }
}
