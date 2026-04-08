package com.aquadev.aiexecutor.service.ai.homework;

import org.springframework.ai.content.Media;

import java.util.List;

public interface HomeworkContextBuilder {

    String buildFullContext(String ocrText, List<Media> images);
}
