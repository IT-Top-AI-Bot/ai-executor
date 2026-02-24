package com.aquadev.ittopaiexecutor.config.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient itTopAiRestClient(
            RestClient.Builder builder,
            @Value("${it-top-ai.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

}

