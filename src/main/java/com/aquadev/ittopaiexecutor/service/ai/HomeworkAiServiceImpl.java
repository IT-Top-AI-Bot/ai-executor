package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.aop.TokenUsageHolder;
import com.aquadev.ittopaiexecutor.dto.AiSolveRequest;
import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.exception.domain.AiResponseParsingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.annotation.NewSpan;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkAiServiceImpl implements HomeworkAiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final TokenUsageHolder tokenUsageHolder;

    @Value("classpath:prompts/homework-system-prompt.md")
    private Resource systemPromptResource;

    @Value("classpath:prompts/homework-user-prompt.md")
    private Resource userPromptResource;

    private PromptTemplate systemPromptTemplate;
    private PromptTemplate userPromptTemplate;

    @PostConstruct
    void initTemplates() {
        systemPromptTemplate = new PromptTemplate(systemPromptResource);
        userPromptTemplate = new PromptTemplate(userPromptResource);
    }

    @Override
    @NewSpan("homework.ai-call")
    public SolvedHomework solve(AiSolveRequest request) {
        boolean hasImages = request.images() != null && !request.images().isEmpty();

        String effectiveSystemPrompt = buildEffectiveSystemPrompt(request.systemPrompt());
        String userMessage = buildUserMessage(request);

        log.debug("AI request — images: {}, systemPrompt: {} chars, userMessage: {} chars",
                hasImages, effectiveSystemPrompt.length(), userMessage.length());

        var requestSpec = chatClient.prompt().system(effectiveSystemPrompt);

        List<Media> images = request.images();
        if (hasImages) {
            requestSpec = requestSpec.user(u -> {
                u.text(userMessage);
                images.forEach(u::media);
            });
        } else {
            requestSpec = requestSpec.user(userMessage);
        }

        var options = MistralAiChatOptions.builder()
                .responseFormat(MistralAiApi.ChatCompletionRequest.ResponseFormat.jsonSchema(SolvedHomework.class))
                .build();
        requestSpec = requestSpec.options(options);

        ChatResponse response = requestSpec.call().chatResponse();

        if (response == null || response.getResult() == null) {
            throw new AiResponseParsingException("AI returned empty response", null);
        }

        tokenUsageHolder.get().addText(response.getMetadata().getUsage());

        String raw = response.getResult().getOutput().getText();
        log.debug("AI response ({} chars):\n{}", raw != null ? raw.length() : 0, raw);

        try {
            return objectMapper.readValue(raw, SolvedHomework.class);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response (jsonLength={}): {}", raw != null ? raw.length() : 0, e.getMessage());
            throw new AiResponseParsingException(
                    "AI returned invalid JSON. parseError=" + e.getClass().getSimpleName(), e);
        }
    }

    private String buildEffectiveSystemPrompt(String userSystemPrompt) {
        String base = systemPromptTemplate.render();
        if (userSystemPrompt != null && !userSystemPrompt.isBlank()) {
            return base + "\n\nДОПОЛНИТЕЛЬНЫЕ ИНСТРУКЦИИ ОТ ПОЛЬЗОВАТЕЛЯ:\n" + userSystemPrompt;
        }
        return base;
    }

    private String buildUserMessage(AiSolveRequest request) {
        String base = userPromptTemplate.render(Map.of(
                "subject", orDefault(request.nameSpec(), "не указан"),
                "theme", orDefault(request.theme(), "не указана"),
                "teacherFio", orDefault(request.teacherFio(), "не указан"),
                "comment", orDefault(request.comment(), "нет"),
                "homework", request.homeworkContext()
        ));

        List<Media> images = request.images();
        if (images == null || images.isEmpty()) {
            return base;
        }
        return base + """
                
                === СКРИНШОТЫ МАКЕТА (%d шт.) — ВОСПРОИЗВЕСТИ ТОЧНО ===
                Прикреплены скриншоты готового результата. Требования:
                1. Эмодзи и unicode-символы — вставить дословно так, как они выглядят на скриншоте
                2. Цвета — использовать точные hex-коды из скриншота, не приблизительные названия
                3. Расположение элементов — воспроизвести точную структуру и позиционирование
                4. Каждый видимый элемент на скриншоте должен присутствовать в коде
                """.formatted(images.size());
    }

    private static String orDefault(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }
}
