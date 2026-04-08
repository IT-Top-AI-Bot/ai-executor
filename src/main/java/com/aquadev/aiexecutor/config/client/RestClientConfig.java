package com.aquadev.aiexecutor.config.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(FileDownloadProperties.class)
public class RestClientConfig {

    @Bean("fileDownloaderRestClient")
    public RestClient fileDownloaderRestClient(
            RestClient.Builder builder,
            FileDownloadProperties properties) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) properties.getConnectTimeout().toMillis());
        requestFactory.setReadTimeout((int) properties.getReadTimeout().toMillis());

        return builder.requestFactory(requestFactory).build();
    }

    @Bean
    public RestClient itTopAiRestClient(
            RestClient.Builder builder,
            @Value("${it-top-ai.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

}

