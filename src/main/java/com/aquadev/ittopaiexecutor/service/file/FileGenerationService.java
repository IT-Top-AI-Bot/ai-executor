package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

public interface FileGenerationService {

    byte[] generateFile(SolvedHomework solved);
}
