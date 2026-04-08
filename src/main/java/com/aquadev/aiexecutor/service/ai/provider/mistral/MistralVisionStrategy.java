package com.aquadev.aiexecutor.service.ai.provider.mistral;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import com.aquadev.aiexecutor.service.ai.strategy.AiVisionStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.content.Media;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MistralVisionStrategy implements AiVisionStrategy {

    private static final int MAX_IMAGES_PER_BATCH = 8;
    private static final long TOKEN_LIMIT_PER_BATCH = 100_000L;
    private static final long IMAGE_TOKEN_FALLBACK = TOKEN_LIMIT_PER_BATCH;

    private final ChatClient mistralChatClient;
    private final TokenUsageHolder tokenUsageHolder;

    @Value("${spring.ai.mistralai.vision.model:mistral-medium-2508}")
    private String visionModel;

    @Value("${spring.ai.mistralai.vision.temperature:0.2}")
    private double visionTemperature;

    @Value("classpath:prompts/vision-system-prompt.md")
    private Resource visionSystemPromptResource;

    @Override
    public AiProviderType providerType() {
        return AiProviderType.MISTRAL;
    }

    @Override
    public String describeImages(List<Media> images) {
        if (images == null || images.isEmpty()) {
            return "";
        }

        log.debug("Sending total {} images to Vision model for processing", images.size());

        List<List<Media>> batches = buildBatches(images);
        log.info("Vision: {} images → {} batches", images.size(), batches.size());

        StringBuilder finalDescription = new StringBuilder();
        String systemPrompt = loadSystemPrompt();

        var options = MistralAiChatOptions.builder()
                .model(visionModel)
                .temperature(visionTemperature)
                .build();

        for (int batchIndex = 0; batchIndex < batches.size(); batchIndex++) {
            List<Media> batch = batches.get(batchIndex);
            int batchNumber = batchIndex + 1;

            int batchBytes = batch.stream().mapToInt(img -> {
                try {
                    return img.getDataAsByteArray().length;
                } catch (Exception e) {
                    return 0;
                }
            }).sum();
            log.info("Vision batch {}/{}: images={}, bytes={}", batchNumber, batches.size(), batch.size(), batchBytes);

            ChatResponse response = mistralChatClient.prompt()
                    .system(systemPrompt)
                    .user(u -> {
                        u.text(String.format("Please describe the provided images in detail. (Part %d)", batchNumber));
                        batch.forEach(u::media);
                    })
                    .options(options)
                    .call()
                    .chatResponse();

            if (response == null || response.getResult() == null) {
                throw new AiResponseParsingException(
                        "Vision AI model returned an empty response for batch " + batchNumber, null);
            }

            tokenUsageHolder.get().addText(response.getMetadata().getUsage());

            String description = response.getResult().getOutput().getText();
            finalDescription.append("--- Описание части ").append(batchNumber).append(" ---\n");
            finalDescription.append(description).append("\n\n");
        }

        String result = finalDescription.toString().trim();
        log.debug("Total recognized text from all images ({} chars)", result.length());
        return result;
    }

    private String loadSystemPrompt() {
        try {
            return visionSystemPromptResource.getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Could not load vision system prompt from classpath, using inline fallback: {}", e.getMessage());
            return "You are an AI assistant. Describe everything in the images in Russian as accurately as possible.";
        }
    }

    private List<List<Media>> buildBatches(List<Media> images) {
        List<List<Media>> batches = new ArrayList<>();
        List<Media> current = new ArrayList<>();
        long currentTokens = 0;

        for (Media image : images) {
            long tokens = estimateImageTokens(image);
            boolean tokenLimitExceeded = currentTokens + tokens > TOKEN_LIMIT_PER_BATCH;
            boolean imageLimitExceeded = current.size() >= MAX_IMAGES_PER_BATCH;
            if (!current.isEmpty() && (tokenLimitExceeded || imageLimitExceeded)) {
                batches.add(current);
                current = new ArrayList<>();
                currentTokens = 0;
            }
            current.add(image);
            currentTokens += tokens;
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    private long estimateImageTokens(Media image) {
        try {
            byte[] bytes = image.getDataAsByteArray();
            try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                if (stream == null) return IMAGE_TOKEN_FALLBACK;
                Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
                if (!readers.hasNext()) return IMAGE_TOKEN_FALLBACK;
                ImageReader reader = readers.next();
                try {
                    reader.setInput(stream);
                    long width = reader.getWidth(0);
                    long height = reader.getHeight(0);
                    long tokens = width * height / 784;
                    log.debug("Image resolution {}x{} → ~{} tokens", width, height, tokens);
                    return tokens;
                } finally {
                    reader.dispose();
                }
            }
        } catch (Exception e) {
            log.warn("Could not read image dimensions, using fallback token estimate: {}", e.getMessage());
            return IMAGE_TOKEN_FALLBACK;
        }
    }
}
