package com.aquadev.ittopaiexecutor.service.file;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;

import java.nio.file.Path;

public interface FileGenerationService {

    Path generateFile(SolvedHomework solved);
}
