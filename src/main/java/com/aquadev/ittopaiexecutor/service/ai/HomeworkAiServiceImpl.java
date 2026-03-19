package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkAiServiceImpl implements HomeworkAiService {

    private static final ObjectMapper RESPONSE_MAPPER = new ObjectMapper();

    private static final Set<String> CODE_KEYWORDS = Set.of(
            "python", "javascript", "typescript", "js", "ts", "html", "css",
            "java", "kotlin", "c#", "c++", "php", "sql", "bash", "shell",
            "программирование", "программировать", "программа", "программный",
            "код", "кодинг", "coding", "скрипт", "script",
            "функция", "алгоритм", "класс", "метод",
            "веб-разработка", "frontend", "backend", "верстка", "вёрстка"
    );

    private final ChatClient chatClient;

    @Value("${mistral.codestral.model:codestral-latest}")
    private String codestralModel;

    @Value("${homework.ai.system-prompt}")
    private String systemPrompt;

    private static final String USER_PROMPT = """
                Выполни практическое задание студента IT-колледжа.
            
                === КОНТЕКСТ ===
                Предмет:       {subject}
                Тема:          {theme}
                Преподаватель: {teacherFio}

                === КОММЕНТАРИЙ ПРЕПОДАВАТЕЛЯ (выполнить строго) ===
                {comment}
            
                === ЗАДАНИЕ ===
        {homework}

        """;

    @Override
    public SolvedHomework solve(String homeworkContext, TokenUsage tokenUsage, String systemPrompt,
                                String theme, String teacherFio, String nameSpec, String comment,
                                List<Media> images) {
        boolean hasImages = images != null && !images.isEmpty();
        boolean codingTask = isCodingTask(theme, nameSpec, homeworkContext);

        String effectiveSystemPrompt = buildEffectiveSystemPrompt(systemPrompt);
        String userMessage = buildUserMessage(homeworkContext, theme, teacherFio, nameSpec, comment, images);

        log.debug("AI request — coding task: {}, images: {}, model: {}",
                codingTask, hasImages, (codingTask && !hasImages) ? codestralModel : "default");
        log.debug("AI request — system prompt ({} chars):\n{}", effectiveSystemPrompt.length(), effectiveSystemPrompt);
        log.debug("AI request — user message ({} chars):\n{}", userMessage.length(), userMessage);

        var requestSpec = chatClient.prompt().system(effectiveSystemPrompt);

        if (hasImages) {
            requestSpec = requestSpec.user(u -> {
                u.text(userMessage);
                images.forEach(u::media);
            });
        } else {
            requestSpec = requestSpec.user(userMessage);
        }

        // Codestral не поддерживает vision — используем только для текстовых задач
        var optionsBuilder = MistralAiChatOptions.builder()
                .responseFormat(MistralAiApi.ChatCompletionRequest.ResponseFormat.jsonSchema(SolvedHomework.class));
        if (codingTask && !hasImages) {
            optionsBuilder = optionsBuilder.model(codestralModel);
        }
        requestSpec = requestSpec.options(optionsBuilder.build());

        ChatResponse response = requestSpec.call().chatResponse();
        tokenUsage.addText(response.getMetadata().getUsage());

        String raw = response.getResult().getOutput().getText();
        log.debug("AI response ({} chars):\n{}", raw != null ? raw.length() : 0, raw);

        try {
            return RESPONSE_MAPPER.readValue(raw, SolvedHomework.class);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response (jsonLength={}): {}", raw != null ? raw.length() : 0, e.getMessage());
            throw new RuntimeException("AI returned invalid JSON. parseError=" + e.getClass().getSimpleName());
        }
    }

    private String buildEffectiveSystemPrompt(String userSystemPrompt) {
        if (userSystemPrompt != null && !userSystemPrompt.isBlank()) {
            return systemPrompt + "\n\nДОПОЛНИТЕЛЬНЫЕ ИНСТРУКЦИИ ОТ ПОЛЬЗОВАТЕЛЯ:\n" + userSystemPrompt;
        }
        return systemPrompt;
    }

    private String buildUserMessage(String homeworkContext, String theme, String teacherFio,
                                    String nameSpec, String comment, List<Media> images) {
        String base = USER_PROMPT
                .replace("{subject}", orDefault(nameSpec, "не указан"))
                .replace("{theme}", orDefault(theme, "не указана"))
                .replace("{teacherFio}", orDefault(teacherFio, "не указан"))
                .replace("{comment}", orDefault(comment, "нет"))
                .replace("{homework}", homeworkContext);

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

    private boolean isCodingTask(String theme, String nameSpec, String homeworkContext) {
        String combined = (
                (theme != null ? theme : "") + " " +
                        (nameSpec != null ? nameSpec : "") + " " +
                        (homeworkContext != null ? homeworkContext.substring(0, Math.min(800, homeworkContext.length())) : "")
        ).toLowerCase();
        return CODE_KEYWORDS.stream().anyMatch(combined::contains);
    }

}
