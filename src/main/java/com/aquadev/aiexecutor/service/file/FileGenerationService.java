package com.aquadev.aiexecutor.service.file;

import com.aquadev.aiexecutor.dto.SolvedHomework;

public interface FileGenerationService {

    byte[] generateFile(SolvedHomework solved);
}
