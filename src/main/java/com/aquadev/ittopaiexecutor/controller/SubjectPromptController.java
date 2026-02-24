package com.aquadev.ittopaiexecutor.controller;

import com.aquadev.ittopaiexecutor.dto.SubjectPromptRequest;
import com.aquadev.ittopaiexecutor.dto.SubjectPromptResponse;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subject-prompts")
@RequiredArgsConstructor
public class SubjectPromptController {

    private final SubjectPromptService service;

    @GetMapping
    public List<SubjectPromptResponse> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubjectPromptResponse create(@Valid @RequestBody SubjectPromptRequest request) {
        return service.save(request);
    }

    @PutMapping("/{specId}")
    public SubjectPromptResponse update(
            @PathVariable Long specId,
            @Valid @RequestBody SubjectPromptRequest request) {
        return service.update(specId, request);
    }

    @DeleteMapping("/{specId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long specId) {
        service.delete(specId);
    }
}
