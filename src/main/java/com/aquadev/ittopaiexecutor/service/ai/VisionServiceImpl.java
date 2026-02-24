package com.aquadev.ittopaiexecutor.service.ai;

import com.aquadev.ittopaiexecutor.dto.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisionServiceImpl implements VisionService {

    private final ChatClient visionChatClient;

    private static final String VISION_PROMPT = """
            Ты анализируешь скриншоты и изображения из PDF с учебным заданием.
            Твоя задача — дать исполнителю максимально точное описание содержимого, чтобы он мог воспроизвести результат.

            Игнорируй полностью: логотипы колледжа, водяные знаки, декоративные фоны, иконки интерфейса.

            Для каждого содержательного изображения опиши:

            ЕСЛИ это скриншот готовой HTML/CSS страницы или её фрагмента:
            - Точная структура: какие элементы есть (таблица, список, форма, заголовки, абзацы)
            - Таблицы: количество строк и столбцов, заголовки, содержимое всех ячеек дословно
            - Цвета: фон страницы, фон ячеек/блоков (например "синий заголовок таблицы", "серые чётные строки")
            - Стили: жирный текст, выравнивание, рамки, отступы, если визуально заметны
            - Текст: дословно весь видимый текст — заголовки, содержимое, подписи

            ЕСЛИ это схема, диаграмма или рисунок:
            - Структура и связи между элементами
            - Все подписи и тексты дословно

            ЕСЛИ это фрагмент кода:
            - Воспроизведи код дословно

            ЕСЛИ это текстовый фрагмент задания или условие:
            - Воспроизведи текст дословно

            Если изображение не несёт полезной информации для выполнения задания — пропусти без упоминания.
            Не пиши фразы "на изображении видно", "картинка показывает" — сразу давай структурированное описание.
            """;

    @Override
    public String describeImages(List<Path> images, TokenUsage tokenUsage, String visionPrompt) {

        if (images == null || images.isEmpty()) {
            return "";
        }

        String effectivePrompt = (visionPrompt != null && !visionPrompt.isBlank()) ? visionPrompt : VISION_PROMPT;

        List<List<Path>> batches = new ArrayList<>();
        int batchSize = 5;

        for (int i = 0; i < images.size(); i += batchSize) {
            int end = Math.min(images.size(), i + batchSize);
            batches.add(images.subList(i, end));
        }

        StringBuilder finalResult = new StringBuilder();

        for (List<Path> batch : batches) {
            String batchResult = describeBatch(batch, tokenUsage, effectivePrompt);
            finalResult.append(batchResult).append("\n\n--- Batch End ---\n\n");
        }

        return finalResult.toString().trim();
    }

    private String describeBatch(List<Path> batch, TokenUsage tokenUsage, String effectivePrompt) {

        var prompt = visionChatClient.prompt();

        prompt.user(u -> {
            u.text(effectivePrompt + "\nКоличество изображений в этом запросе: " + batch.size());

            for (int i = 0; i < batch.size(); i++) {
                u.text("[Изображение " + (i + 1) + "]");
                u.media(MediaType.IMAGE_PNG, new FileSystemResource(batch.get(i)));
            }
        });

        ChatResponse response = prompt.call().chatResponse();
        tokenUsage.addVision(response.getMetadata().getUsage());
        return response.getResult().getOutput().getText();
    }
}
