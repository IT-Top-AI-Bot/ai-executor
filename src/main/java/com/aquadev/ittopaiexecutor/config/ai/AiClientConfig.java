package com.aquadev.ittopaiexecutor.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Configuration
public class AiClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClientCustomizer bufferingRestClientCustomizer() {
        return builder -> builder.requestFactory(
                new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()));
    }
}
