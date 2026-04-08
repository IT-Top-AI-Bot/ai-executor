package com.aquadev.aiexecutor.service.ai.strategy;

import com.aquadev.aiexecutor.dto.AiProviderType;
import org.springframework.ai.content.Media;

import java.util.List;

public interface AiVisionStrategy {

    String describeImages(List<Media> images);

    AiProviderType providerType();
}
