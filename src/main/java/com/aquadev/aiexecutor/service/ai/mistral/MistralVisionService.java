package com.aquadev.aiexecutor.service.ai.mistral;

import org.springframework.ai.content.Media;

import java.util.List;

public interface MistralVisionService {

    String describeImages(List<Media> images);
}
