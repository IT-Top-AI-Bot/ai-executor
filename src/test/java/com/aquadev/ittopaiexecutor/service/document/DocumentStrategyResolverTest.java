package com.aquadev.ittopaiexecutor.service.document;

import com.aquadev.ittopaiexecutor.exception.domain.UnsupportedDocumentTypeException;
import com.aquadev.ittopaiexecutor.service.document.extractor.DocumentExtractor;
import com.aquadev.ittopaiexecutor.service.document.extractor.TxtDocumentExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentStrategyResolverTest {

    private DocumentStrategyResolver resolver;
    private TxtDocumentExtractor txtExtractor;

    @BeforeEach
    void setUp() {
        txtExtractor = new TxtDocumentExtractor();

        // Custom mock extractor for pdf
        DocumentExtractor pdfExtractor = mock(DocumentExtractor.class);
        when(pdfExtractor.supportedMimeTypes()).thenReturn(java.util.Set.of("pdf"));

        resolver = new DocumentStrategyResolver(List.of(txtExtractor, pdfExtractor));
        resolver.init();
    }

    @Test
    void resolve_txtFile_returnsTxtExtractor() {
        DocumentExtractor result = resolver.resolve("document.txt");
        assertThat(result).isSameAs(txtExtractor);
    }

    @Test
    void resolve_upperCaseExtension_resolvesCaseInsensitively() {
        DocumentExtractor result = resolver.resolve("document.TXT");
        assertThat(result).isSameAs(txtExtractor);
    }

    @Test
    void resolve_pdfFile_returnsPdfExtractor() {
        DocumentExtractor result = resolver.resolve("homework.pdf");
        assertThat(result).isNotNull();
    }

    @Test
    void resolve_unsupportedExtension_throwsUnsupportedDocumentTypeException() {
        assertThatThrownBy(() -> resolver.resolve("archive.zip"))
                .isInstanceOf(UnsupportedDocumentTypeException.class)
                .hasMessageContaining("zip");
    }

    @Test
    void resolve_noExtension_throwsUnsupportedDocumentTypeException() {
        assertThatThrownBy(() -> resolver.resolve("fileWithNoExtension"))
                .isInstanceOf(UnsupportedDocumentTypeException.class);
    }

    @Test
    void resolve_fileWithPath_usesOnlyExtension() {
        DocumentExtractor result = resolver.resolve("/some/path/to/document.txt");
        assertThat(result).isSameAs(txtExtractor);
    }
}
