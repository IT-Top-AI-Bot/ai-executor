package com.aquadev.aiexecutor.service.ai.ocr;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.exception.domain.OcrFailedException;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class GeminiVisionOcrStrategy implements OcrStrategy {

    private static final Set<String> SUPPORTED = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/tiff",
            "image/avif",
            "image/heic",
            "image/heif"
    );

    private final ChatClient geminiChatClient;
    private final TokenUsageHolder tokenUsageHolder;

    @Value("${spring.ai.google.genai.vision.model:gemini-2.0-flash}")
    private String visionModel;

    @Value("classpath:prompts/ocr-system-prompt.md")
    private Resource ocrSystemPrompt;

    public GeminiVisionOcrStrategy(ChatClient geminiChatClient, TokenUsageHolder tokenUsageHolder) {
        this.geminiChatClient = geminiChatClient;
        this.tokenUsageHolder = tokenUsageHolder;
    }

    @Override
    public Set<String> supportedMimeTypes() {
        return SUPPORTED;
    }

    @Override
    @Retry(name = "gemini-ocr")
    public OcrResult extract(byte[] content, String mimeType) {
        log.debug("Extracting text via Gemini Vision OCR: mimeType={}, size={} bytes", mimeType, content.length);

        var media = new Media(MimeType.valueOf(mimeType), new ByteArrayResource(content));
        var options = GoogleGenAiChatOptions.builder()
                .model(visionModel)
                .temperature(0.1)
                .build();

        var response = geminiChatClient.prompt()
                .system(ocrSystemPrompt)
                .user(u -> {
                    u.text("Extract all text from this image.");
                    u.media(media);
                })
                .options(options)
                .call()
                .chatResponse();

        if (response == null || response.getResult() == null) {
            throw new OcrFailedException("Gemini Vision OCR returned empty response for mimeType=" + mimeType);
        }

        tokenUsageHolder.get().addText(response.getMetadata().getUsage());

        String text = response.getResult().getOutput().getText();
        log.debug("Gemini Vision OCR complete: chars={}", text != null ? text.length() : 0);
        return new OcrResult(text != null ? text : "", List.of());
    }
}
