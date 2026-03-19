package com.aquadev.ittopaiexecutor.service.prompt;

import com.aquadev.ittopaiexecutor.dto.SubjectPromptRequest;
import com.aquadev.ittopaiexecutor.dto.SubjectPromptResponse;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;

import java.util.List;
import java.util.Optional;

public interface SubjectPromptService {

    SubjectPromptResponse save(SubjectPromptRequest request);

    SubjectPromptResponse upsert(Long specId, SubjectPromptRequest request);

    SubjectPromptResponse update(Long specId, SubjectPromptRequest request);

    void delete(Long specId);

    List<SubjectPromptResponse> findAll();

    Optional<SubjectPrompt> findBySpecId(Long specId);
}
