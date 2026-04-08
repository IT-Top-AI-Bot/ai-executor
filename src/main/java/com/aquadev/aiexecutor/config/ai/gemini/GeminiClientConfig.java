package com.aquadev.aiexecutor.config.ai.gemini;

import com.google.genai.Client;
import com.google.genai.types.ClientOptions;
import com.google.genai.types.ProxyOptions;
import com.google.genai.types.ProxyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.AdvisorParams;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(GeminiHttpProperties.class)
public class GeminiClientConfig {

    @Bean
    public ChatClient geminiChatClient(
            GeminiHttpProperties props,
            @Value("${spring.ai.google.genai.api-key}") String apiKey
    ) {
        Client.Builder clientBuilder = Client.builder()
                .apiKey(apiKey)
                .vertexAI(false);

        if (props.proxyHost() != null && !props.proxyHost().isBlank()) {
            log.info("Gemini proxy: {}:{}", props.proxyHost(), props.proxyPort());
            clientBuilder.clientOptions(ClientOptions.builder()
                    .proxyOptions(ProxyOptions.builder()
                            .type(ProxyType.Known.HTTP)
                            .host(props.proxyHost())
                            .port(props.proxyPort())
                            .build())
                    .build());
        }

        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(clientBuilder.build())
                .build();

        return ChatClient.builder(model)
                .defaultAdvisors(AdvisorParams.ENABLE_NATIVE_STRUCTURED_OUTPUT)
                .build();
    }
}
