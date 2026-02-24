package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.SolvedHomework;
import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class HomeworkAiServiceImpl implements HomeworkAiService {

    private final ChatModel chatModel;
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

    private static final String USER_PROMPT_TEXT_ONLY = """
        Выполни практическое задание:

        {homework}
        """;

    private static final String USER_PROMPT_WITH_IMAGES = """
        Выполни практическое задание.

        ТЕКСТ ЗАДАНИЯ:
        {homework}

        ВИЗУАЛЬНЫЕ МАТЕРИАЛЫ:
        К заданию прикреплено {count} изображени(е/я). Они показывают, как должен выглядеть результат,
        или содержат дополнительные условия. Изучи их внимательно и воспроизведи максимально точно.
        """;

    @Override
    public SolvedHomework solve(String homeworkContext, TokenUsage tokenUsage, String systemPrompt) {

    }
}
