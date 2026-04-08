package com.aquadev.aiexecutor.service.prompt;

import com.aquadev.aiexecutor.dto.SubjectPromptRequest;
import com.aquadev.aiexecutor.dto.SubjectPromptResponse;
import com.aquadev.aiexecutor.model.SubjectPrompt;

import java.util.List;
import java.util.Optional;

public interface SubjectPromptService {

    SubjectPromptResponse save(Long telegramUserId, SubjectPromptRequest request);

    SubjectPromptResponse upsert(Long telegramUserId, Long specId, SubjectPromptRequest request);

    SubjectPromptResponse update(Long telegramUserId, Long specId, SubjectPromptRequest request);

    void delete(Long telegramUserId, Long specId);

    List<SubjectPromptResponse> findAll(Long telegramUserId);

    Optional<SubjectPrompt> findByTelegramUserIdAndSpecId(Long telegramUserId, Long specId);
}
