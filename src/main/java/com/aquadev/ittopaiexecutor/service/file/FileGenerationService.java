package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface FileGenerationService {

    /**
     * Serialises the solved homework into raw bytes ready for storage.
     * For {@code .docx} extension the bytes represent a valid OOXML document;
     * for all other types the content string is UTF-8 encoded.
     */
    byte[] generateFile(SolvedHomework solved);
}
