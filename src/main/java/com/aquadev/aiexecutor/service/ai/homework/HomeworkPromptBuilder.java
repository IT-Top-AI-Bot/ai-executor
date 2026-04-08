package com.aquadev.aiexecutor.service.ai.homework;

import com.aquadev.aiexecutor.dto.AiSolveRequest;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class HomeworkPromptBuilder {

    @Value("classpath:prompts/homework-system-prompt.md")
    private Resource systemPromptResource;

    @Value("classpath:prompts/homework-user-prompt.md")
    private Resource userPromptResource;

    @Value("classpath:prompts/homework-image-suffix-prompt.md")
    private Resource imageSuffixResource;

    private String systemPromptText;
    private PromptTemplate userPromptTemplate;
    private PromptTemplate imageSuffixTemplate;

    @PostConstruct
    void init() {
        try {
            systemPromptText = systemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load system prompt", e);
        }
        userPromptTemplate = new PromptTemplate(userPromptResource);
        imageSuffixTemplate = new PromptTemplate(imageSuffixResource);
    }

    public String buildSystemPrompt(String userSystemPrompt) {
        if (userSystemPrompt != null && !userSystemPrompt.isBlank()) {
            return systemPromptText + "\n\nДОПОЛНИТЕЛЬНЫЕ ИНСТРУКЦИИ ОТ ПОЛЬЗОВАТЕЛЯ:\n" + userSystemPrompt;
        }
        return systemPromptText;
    }

    public String buildUserMessage(AiSolveRequest request) {
        String base = userPromptTemplate.render(Map.of(
                "subject", orDefault(request.nameSpec(), "не указан"),
                "theme", orDefault(request.theme(), "не указана"),
                "teacherFio", orDefault(request.teacherFio(), "не указан"),
                "comment", orDefault(request.comment(), "нет"),
                "homework", request.homeworkContext()
        ));

        var images = request.images();
        if (images == null || images.isEmpty()) {
            return base;
        }

        String suffix = imageSuffixTemplate.render(Map.of("imageCount", images.size()));
        return base + "\n" + suffix;
    }

    private static String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
