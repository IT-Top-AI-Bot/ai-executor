package com.aquadev.ittopaiexecutor.service.prompt;

import com.aquadev.ittopaiexecutor.dto.SubjectPromptRequest;
import com.aquadev.ittopaiexecutor.dto.SubjectPromptResponse;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.repository.SubjectPromptRepository;
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
    public SubjectPromptResponse save(SubjectPromptRequest request) {
        if (repository.existsById(request.specId())) {
            throw new IllegalArgumentException("Prompt for specId=" + request.specId() + " already exists. Use PUT to update.");
        }
        SubjectPrompt entity = toEntity(request);
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubjectPromptResponse upsert(Long specId, SubjectPromptRequest request) {
        SubjectPrompt entity = repository.findById(specId).orElseGet(SubjectPrompt::new);
        entity.setSpecId(specId);
        entity.setNameSpec(request.nameSpec());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setVisionPrompt(request.visionPrompt());
        entity.setStaticText(request.staticText());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public SubjectPromptResponse update(Long specId, SubjectPromptRequest request) {
        SubjectPrompt entity = repository.findById(specId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt for specId=" + specId + " not found"));
        entity.setNameSpec(request.nameSpec());
        entity.setSystemPrompt(request.systemPrompt());
        entity.setVisionPrompt(request.visionPrompt());
        entity.setStaticText(request.staticText());
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long specId) {
        repository.deleteById(specId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubjectPromptResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubjectPrompt> findBySpecId(Long specId) {
        return repository.findById(specId);
    }

    private SubjectPrompt toEntity(SubjectPromptRequest req) {
        SubjectPrompt e = new SubjectPrompt();
        e.setSpecId(req.specId());
        e.setNameSpec(req.nameSpec());
        e.setSystemPrompt(req.systemPrompt());
        e.setVisionPrompt(req.visionPrompt());
        e.setStaticText(req.staticText());
        return e;
    }

    private SubjectPromptResponse toResponse(SubjectPrompt e) {
        return new SubjectPromptResponse(e.getSpecId(), e.getNameSpec(), e.getSystemPrompt(), e.getVisionPrompt(), e.getStaticText());
    }
}
