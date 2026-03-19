package com.aquadev.ittopaiexecutor.service.homework;

import com.aquadev.ittopaiexecutor.dto.ExtractedDocument;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.service.ai.HomeworkAiService;
import com.aquadev.ittopaiexecutor.service.document.DocumentStrategyResolver;
import com.aquadev.ittopaiexecutor.service.document.extractor.DocumentExtractor;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeworkSolverImplTest {

    @Mock
    ChatClient chatClient;
    @Mock
    DocumentStrategyResolver strategyResolver;
    @Mock
    HomeworkAiService homeworkAiService;
    @Mock
    SubjectPromptService subjectPromptService;

    @InjectMocks
    HomeworkSolverImpl solver;

    // ── solve: with custom prompt ─────────────────────────────────────────────

    @Test
    void solve_withCustomPrompt_usesCustomSystemPrompt() {
        byte[] content = "homework text".getBytes();
        String filename = "hw.txt";
        Long specId = 5L;

        DocumentExtractor extractor = mock(DocumentExtractor.class);
        ExtractedDocument extracted = new ExtractedDocument("extracted text", Map.of());
        when(strategyResolver.resolve(filename)).thenReturn(extractor);
        when(extractor.extract(content, filename, chatClient)).thenReturn(extracted);

        SubjectPrompt customPrompt = new SubjectPrompt();
        customPrompt.setSpecId(specId);
        customPrompt.setSystemPrompt("Custom system prompt for Math");
        when(subjectPromptService.findBySpecId(specId)).thenReturn(Optional.of(customPrompt));

        SolvedHomework expected = new SolvedHomework("result", "docx", "Answer content");
        when(homeworkAiService.solve(eq("extracted text"), any(TokenUsage.class), eq("Custom system prompt for Math")))
                .thenReturn(expected);

        SolvedHomework result = solver.solve(content, filename, specId);

        assertThat(result).isSameAs(expected);
        verify(homeworkAiService).solve(eq("extracted text"), any(), eq("Custom system prompt for Math"));
    }

    // ── solve: without custom prompt ─────────────────────────────────────────

    @Test
    void solve_noCustomPrompt_usesNullSystemPrompt() {
        byte[] content = "text".getBytes();
        String filename = "hw.txt";
        Long specId = 99L;

        DocumentExtractor extractor = mock(DocumentExtractor.class);
        when(strategyResolver.resolve(filename)).thenReturn(extractor);
        when(extractor.extract(content, filename, chatClient))
                .thenReturn(new ExtractedDocument("text", Map.of()));

        when(subjectPromptService.findBySpecId(specId)).thenReturn(Optional.empty());

        SolvedHomework expected = new SolvedHomework("result", "txt", "Answer");
        when(homeworkAiService.solve(eq("text"), any(TokenUsage.class), isNull()))
                .thenReturn(expected);

        SolvedHomework result = solver.solve(content, filename, specId);

        assertThat(result).isSameAs(expected);
        verify(homeworkAiService).solve(eq("text"), any(), isNull());
    }

    // ── solve: extraction error propagates ───────────────────────────────────

    @Test
    void solve_extractionFails_exceptionPropagates() {
        byte[] content = "bad".getBytes();
        String filename = "bad.xyz";

        DocumentExtractor extractor = mock(DocumentExtractor.class);
        when(strategyResolver.resolve(filename)).thenReturn(extractor);
        when(extractor.extract(content, filename, chatClient))
                .thenThrow(new RuntimeException("Parse error"));

        assertThatThrownBy(() -> solver.solve(content, filename, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Parse error");

        verifyNoInteractions(homeworkAiService);
    }

    // ── solve: strategy resolution ────────────────────────────────────────────

    @Test
    void solve_delegatesFilenameToStrategyResolver() {
        byte[] content = "data".getBytes();
        String filename = "report.pdf";

        DocumentExtractor extractor = mock(DocumentExtractor.class);
        when(strategyResolver.resolve(filename)).thenReturn(extractor);
        when(extractor.extract(content, filename, chatClient))
                .thenReturn(new ExtractedDocument("text", Map.of()));
        when(subjectPromptService.findBySpecId(any())).thenReturn(Optional.empty());
        when(homeworkAiService.solve(any(), any(), any()))
                .thenReturn(new SolvedHomework("f", "pdf", "c"));

        solver.solve(content, filename, 1L);

        verify(strategyResolver).resolve("report.pdf");
    }
}
