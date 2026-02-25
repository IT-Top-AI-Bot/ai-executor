package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkAiServiceImpl implements HomeworkAiService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        Ты опытный исполнитель учебных практических заданий.
        Твоя задача — выполнить задание и вернуть готовый файл.

        ШАГ 1 — Определи формат выходного файла по смыслу задания:
        • HTML/CSS/JS вёрстка, скриншоты страниц, таблицы с оформлением → "html"
        • Программирование на Python → "py"
        • Программирование на JavaScript (без HTML) → "js"
        • Текстовое задание: вопросы, ответы, реферат, описание → "docx"
        • Табличные данные, расчёты без оформления → "txt"
        • Любой другой код → выбери расширение по языку

        ШАГ 2 — Выполни задание полностью:
        • HTML: воспроизведи структуру, цвета, таблицы, текст максимально точно по скриншотам
        • Текст: развёрнутые грамотные ответы на каждый пункт, полными предложениями
        • Код: рабочий код строго по условию, без лишних импортов

        Строгие правила:
        • Поле content — только содержимое файла, готовое к сдаче, без пояснений
        • Никаких комментариев в коде (HTML/CSS/JS/py)
        • Несколько заданий → всё в одном файле последовательно
        • Никакого markdown внутри content (если это не .md файл)

        Формат ответа — строго валидный JSON без markdown-обёртки:
        {
          "filename": "<название темы латиницей, snake_case, без пробелов>",
          "extension": "<расширение файла>",
          "content": "<полное содержимое файла>"
        }
        """;

    private static final String USER_PROMPT = """
        Выполни практическое задание:

        {homework}
        """;

    @Override
    public SolvedHomework solve(String homeworkContext, TokenUsage tokenUsage, String systemPrompt) {
        String effectiveSystemPrompt = (systemPrompt != null && !systemPrompt.isBlank())
                ? systemPrompt
                : SYSTEM_PROMPT;

        String userMessage = USER_PROMPT.replace("{homework}", homeworkContext);

        ChatResponse response = chatClient.prompt()
                .system(effectiveSystemPrompt)
                .user(userMessage)
                .call()
                .chatResponse();

        tokenUsage.addText(response.getMetadata().getUsage());

        String raw = response.getResult().getOutput().getText();
        log.debug("Raw AI response length={}", raw != null ? raw.length() : 0);

        String json = stripMarkdownFence(raw);

        try {
            return objectMapper.readValue(json, SolvedHomework.class);
        } catch (Exception e) {
            // НЕ прокидываем e как cause — Jackson кладёт весь raw-ответ (~1 MB) в getMessage(),
            // что приводит к RecordTooLargeException при отправке FAILED-события в Kafka.
            log.error("Failed to parse AI response as JSON (jsonLength={}): {}", json.length(), e.getMessage());
            throw new RuntimeException("AI returned invalid JSON. jsonLength=%d, parseError=%s"
                    .formatted(json.length(), e.getClass().getSimpleName()));
        }
    }

    private String stripMarkdownFence(String text) {
        if (text == null) return "{}";
        String trimmed = text.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }
}
