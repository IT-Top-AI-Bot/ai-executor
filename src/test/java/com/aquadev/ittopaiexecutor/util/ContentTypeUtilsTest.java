package com.aquadev.ittopaiexecutor.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ContentTypeUtilsTest {

    // ── forExtension ──────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "pdf,   application/pdf",
            "docx,  application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "txt,   text/plain; charset=utf-8",
            "html,  text/html; charset=utf-8",
            "json,  application/json",
            "png,   image/png",
            "jpg,   image/jpeg",
            "jpeg,  image/jpeg"
    })
    void forExtension_knownExtensions_returnCorrectMimeType(String ext, String expectedMime) {
        assertThat(ContentTypeUtils.forExtension(ext)).isEqualTo(expectedMime);
    }

    @Test
    void forExtension_upperCase_returnsCorrectMimeType() {
        assertThat(ContentTypeUtils.forExtension("PDF")).isEqualTo("application/pdf");
        assertThat(ContentTypeUtils.forExtension("DOCX"))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void forExtension_unknown_returnsOctetStream() {
        assertThat(ContentTypeUtils.forExtension("xyz")).isEqualTo("application/octet-stream");
        assertThat(ContentTypeUtils.forExtension("bin")).isEqualTo("application/octet-stream");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void forExtension_nullOrBlank_returnsOctetStream(String ext) {
        assertThat(ContentTypeUtils.forExtension(ext)).isEqualTo("application/octet-stream");
    }

    // ── forFilename ───────────────────────────────────────────────────────────

    @Test
    void forFilename_pdfFile_returnsPdfMimeType() {
        assertThat(ContentTypeUtils.forFilename("document.pdf")).isEqualTo("application/pdf");
    }

    @Test
    void forFilename_docxFile_returnsDocxMimeType() {
        assertThat(ContentTypeUtils.forFilename("report.docx"))
                .isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Test
    void forFilename_noExtension_returnsOctetStream() {
        assertThat(ContentTypeUtils.forFilename("noextension")).isEqualTo("application/octet-stream");
    }

    @Test
    void forFilename_null_returnsOctetStream() {
        assertThat(ContentTypeUtils.forFilename(null)).isEqualTo("application/octet-stream");
    }

    @Test
    void forFilename_pathWithMultipleDots_usesLastExtension() {
        assertThat(ContentTypeUtils.forFilename("my.report.final.pdf")).isEqualTo("application/pdf");
    }

    // ── extensionForContentType ───────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
            "application/pdf,                                                             pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document,    docx",
            "application/msword,                                                          doc",
            "text/plain,                                                                  txt"
    })
    void extensionForContentType_knownTypes_returnsExtension(String contentType, String expectedExt) {
        assertThat(ContentTypeUtils.extensionForContentType(contentType)).isEqualTo(expectedExt);
    }

    @Test
    void extensionForContentType_withCharsetParam_stripsParamBeforeLookup() {
        assertThat(ContentTypeUtils.extensionForContentType("text/plain; charset=utf-8")).isEqualTo("txt");
    }

    @Test
    void extensionForContentType_unknown_returnsNull() {
        assertThat(ContentTypeUtils.extensionForContentType("application/json")).isNull();
        assertThat(ContentTypeUtils.extensionForContentType("image/png")).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void extensionForContentType_nullOrBlank_returnsNull(String contentType) {
        assertThat(ContentTypeUtils.extensionForContentType(contentType)).isNull();
    }
}
