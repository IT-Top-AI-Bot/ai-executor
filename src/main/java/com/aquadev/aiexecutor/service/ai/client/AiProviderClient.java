package com.aquadev.aiexecutor.service.ai.client;

import com.aquadev.aiexecutor.dto.AiProviderType;
import com.aquadev.aiexecutor.dto.AiRequest;

public interface AiProviderClient {

    <T> T execute(AiRequest<T> request);

    AiProviderType providerType();
}
