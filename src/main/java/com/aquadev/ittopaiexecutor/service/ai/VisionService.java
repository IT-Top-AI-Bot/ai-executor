package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.TokenUsage;

import java.nio.file.Path;
import java.util.List;

public interface VisionService {

    String describeImages(List<Path> images, TokenUsage tokenUsage, String visionPrompt);
}
