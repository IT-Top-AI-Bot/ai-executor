package com.aquadev.aiexecutor.controller;

import com.aquadev.aiexecutor.dto.SubjectPromptRequest;
import com.aquadev.aiexecutor.dto.SubjectPromptResponse;
import com.aquadev.aiexecutor.model.SubjectPrompt;
import com.aquadev.aiexecutor.service.prompt.SubjectPromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subject-prompts")
@RequiredArgsConstructor
public class SubjectPromptController {

    private final SubjectPromptService service;

    @GetMapping
    public List<SubjectPromptResponse> getAll() {
        return service.findAll(currentUserId());
    }

    @GetMapping("/{specId}")
    public SubjectPromptResponse getBySpecId(@PathVariable Long specId) {
        return service.findByTelegramUserIdAndSpecId(currentUserId(), specId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prompt not found for specId=" + specId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectPromptResponse create(@Valid @RequestBody SubjectPromptRequest request) {
        return service.save(currentUserId(), request);
    }

    @PutMapping("/{specId}")
    public SubjectPromptResponse upsert(
            @PathVariable Long specId,
            @Valid @RequestBody SubjectPromptRequest request) {
        return service.upsert(currentUserId(), specId, request);
    }

    @DeleteMapping("/{specId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long specId) {
        service.delete(currentUserId(), specId);
    }

    private static Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private SubjectPromptResponse toResponse(SubjectPrompt p) {
        return new SubjectPromptResponse(p.getSpecId(), p.getNameSpec(), p.getSystemPrompt(), p.getStaticText());
    }
}
