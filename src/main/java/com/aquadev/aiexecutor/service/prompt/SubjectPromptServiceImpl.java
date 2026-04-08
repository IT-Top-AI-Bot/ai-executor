package com.aquadev.aiexecutor.service.prompt;

import com.aquadev.aiexecutor.dto.SubjectPromptRequest;
import com.aquadev.aiexecutor.dto.SubjectPromptResponse;
import com.aquadev.aiexecutor.model.SubjectPrompt;
import com.aquadev.aiexecutor.repository.SubjectPromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubjectPromptServiceImpl implements SubjectPromptService {

    private final SubjectPromptRepository repository;

    @Override
    @Transactional
    public SubjectPromptResponse save(Long telegramUserId, SubjectPromptRequest request) {
        if (repository.existsByTelegramUserIdAndSpecId(telegramUserId, request.specId())) {
            throw new IllegalArgumentException(
                    "Prompt for specId=" + request.specId() + " already exists. Use PUT to update.");
        }
        SubjectPrompt entity = toEntity(telegramUserId, request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubjectPromptResponse upsert(Long telegramUserId, Long specId, SubjectPromptRequest request) {
        SubjectPrompt entity = repository.findByTelegramUserIdAndSpecId(telegramUserId, specId)
                .orElseGet(SubjectPrompt::new);
        entity.setTelegramUserId(telegramUserId);
        entity.setSpecId(specId);
        entity.setNameSpec(request.nameSpec());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setStaticText(request.staticText());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubjectPromptResponse update(Long telegramUserId, Long specId, SubjectPromptRequest request) {
        SubjectPrompt entity = repository.findByTelegramUserIdAndSpecId(telegramUserId, specId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt for specId=" + specId + " not found"));
        entity.setNameSpec(request.nameSpec());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setStaticText(request.staticText());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long telegramUserId, Long specId) {
        repository.deleteByTelegramUserIdAndSpecId(telegramUserId, specId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectPromptResponse> findAll(Long telegramUserId) {
        return repository.findAllByTelegramUserId(telegramUserId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubjectPrompt> findByTelegramUserIdAndSpecId(Long telegramUserId, Long specId) {
        return repository.findByTelegramUserIdAndSpecId(telegramUserId, specId);
    }

    private SubjectPrompt toEntity(Long telegramUserId, SubjectPromptRequest req) {
        SubjectPrompt e = new SubjectPrompt();
        e.setTelegramUserId(telegramUserId);
        e.setSpecId(req.specId());
        e.setNameSpec(req.nameSpec());
        e.setSystemPrompt(req.systemPrompt());
        e.setStaticText(req.staticText());
        return e;
    }

    private SubjectPromptResponse toResponse(SubjectPrompt e) {
        return new SubjectPromptResponse(e.getSpecId(), e.getNameSpec(), e.getSystemPrompt(), e.getStaticText());
    }
}
