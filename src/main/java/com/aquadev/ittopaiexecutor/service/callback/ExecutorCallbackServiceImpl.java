package com.aquadev.ittopaiexecutor.service.callback;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutorCallbackServiceImpl implements ExecutorCallbackService {

    private final RestClient itTopAiRestClient;

    @Override
    public void notifyCompleted(String executionId, String s3Key) {
        log.info("Sending completion callback for executionId={}, s3Key={}", executionId, s3Key);

        itTopAiRestClient.patch()
                .uri("/api/v1/internal/homework-executions/{id}/complete", executionId)
                .body(Map.of("s3Key", s3Key))
                .retrieve()
                .toBodilessEntity();

        log.info("Callback sent successfully for executionId={}", executionId);
    }
}
