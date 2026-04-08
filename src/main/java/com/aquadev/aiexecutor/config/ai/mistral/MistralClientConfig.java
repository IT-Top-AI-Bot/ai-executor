package com.aquadev.aiexecutor.config.ai.mistral;

import com.aquadev.aiexecutor.dto.AiProviderType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mistralai.MistralAiChatModel;
import org.springframework.ai.mistralai.api.MistralAiApi;
import org.springframework.ai.mistralai.ocr.MistralOcrApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MistralHttpProperties.class)
public class MistralClientConfig {

    @Bean
    public MistralOcrApi mistralOcrApi(
            MistralHttpProperties props,
            @Value("${spring.ai.mistralai.api-key}") String apiKey,
            @Value("${spring.ai.mistralai.base-url:https://api.mistral.ai}") String baseUrl
    ) {
        RestClient.Builder restClientBuilder = buildRestClient(
                props.connectTimeout(), props.readTimeout(),
                props.proxyHost(), props.proxyPort(),
                AiProviderType.MISTRAL.name() + "-OCR"
        );
        return new MistralOcrApi(baseUrl, apiKey, restClientBuilder);
    }

    @Bean
    public ChatClient mistralChatClient(
            MistralHttpProperties props,
            @Value("${spring.ai.mistralai.api-key}") String apiKey,
            @Value("${spring.ai.mistralai.base-url:https://api.mistral.ai}") String baseUrl
    ) {
        RestClient.Builder restClientBuilder = buildRestClient(
                props.connectTimeout(), props.readTimeout(),
                props.proxyHost(), props.proxyPort(),
                AiProviderType.MISTRAL.name()
        );

        MistralAiApi mistralApi = MistralAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .restClientBuilder(restClientBuilder)
                .build();

        MistralAiChatModel model = MistralAiChatModel.builder()
                .mistralAiApi(mistralApi)
                .build();

        return ChatClient.builder(model).build();
    }

    private RestClient.Builder buildRestClient(
            Duration connectTimeout,
            Duration readTimeout,
            String proxyHost,
            int proxyPort,
            String providerName
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        if (proxyHost != null && !proxyHost.isBlank()) {
            log.info("{} HTTP proxy: {}:{}", providerName, proxyHost, proxyPort);
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }

        return RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(factory))
                .requestInterceptor(errorLoggingInterceptor(providerName));
    }

    private ClientHttpRequestInterceptor errorLoggingInterceptor(String provider) {
        return (request, body, execution) -> {
            var response = execution.execute(request, body);
            if (!response.getStatusCode().is2xxSuccessful()) {
                try {
                    String responseBody = new String(response.getBody().readAllBytes());
                    log.error("{} API error: status={}, body={}", provider, response.getStatusCode(), responseBody);
                } catch (IOException e) {
                    log.error("{} API error: status={}, <failed to read body>", provider, response.getStatusCode());
                }
            }
            return response;
        };
    }
}
