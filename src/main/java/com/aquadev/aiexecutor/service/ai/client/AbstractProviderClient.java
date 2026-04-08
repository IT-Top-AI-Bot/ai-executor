package com.aquadev.aiexecutor.service.ai.client;

import com.aquadev.aiexecutor.aop.TokenUsageHolder;
import com.aquadev.aiexecutor.dto.AiRequest;
import com.aquadev.aiexecutor.exception.domain.AiResponseParsingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractProviderClient implements AiProviderClient {

    private final ChatClient chatClient;
    private final TokenUsageHolder tokenUsageHolder;

    protected <T> T doExecute(AiRequest<T> request) {
        var callResponse = chatClient.prompt()
                .system(request.systemPrompt())
                .user(u -> {
                    u.text(request.userMessage());
                    if (request.images() != null) {
                        request.images().forEach(u::media);
                    }
                })
                .options(request.options())
                .call();


        ChatResponse response = callResponse.chatResponse();

        if (response == null || response.getResult() == null) {
            throw new AiResponseParsingException(providerType() + " returned empty response", null);
        }

        Usage usage = response.getMetadata().getUsage();

        tokenUsageHolder.get().addText(usage);
        String text = response.getResult().getOutput().getText();
        log.debug("Gemini response ({} chars)", text != null ? text.length() : 0);

        return callResponse.entity(request.clazz());
    }
}
