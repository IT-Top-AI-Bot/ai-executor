package com.aquadev.ittopaiexecutor.service.prompt;

import com.aquadev.ittopaiexecutor.dto.SubjectPromptRequest;
import com.aquadev.ittopaiexecutor.dto.SubjectPromptResponse;
import com.aquadev.ittopaiexecutor.entity.SubjectPrompt;
import com.aquadev.ittopaiexecutor.repository.SubjectPromptRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubjectPromptServiceImplTest {

    @Mock
    SubjectPromptRepository repository;

    @InjectMocks
    SubjectPromptServiceImpl service;

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    void save_newPrompt_savesAndReturnsResponse() {
        SubjectPromptRequest request = new SubjectPromptRequest(1L, "Math", "Solve this", "Describe this", null);
        when(repository.existsById(1L)).thenReturn(false);

        SubjectPrompt saved = buildPrompt(1L, "Math", "Solve this", "Describe this");
        when(repository.save(any())).thenReturn(saved);

        SubjectPromptResponse response = service.save(request);

        assertThat(response.specId()).isEqualTo(1L);
        assertThat(response.nameSpec()).isEqualTo("Math");
        assertThat(response.systemPrompt()).isEqualTo("Solve this");
        assertThat(response.visionPrompt()).isEqualTo("Describe this");
    }

    @Test
    void save_duplicateSpecId_throwsIllegalArgument() {
        when(repository.existsById(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.save(new SubjectPromptRequest(1L, "Math", "P", "V", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists")
                .hasMessageContaining("PUT");
    }

    @Test
    void save_setsAllFieldsOnEntity() {
        SubjectPromptRequest request = new SubjectPromptRequest(5L, "Physics", "Sys prompt", "Vis prompt", null);
        when(repository.existsById(5L)).thenReturn(false);

        SubjectPrompt saved = buildPrompt(5L, "Physics", "Sys prompt", "Vis prompt");
        when(repository.save(any())).thenReturn(saved);

        service.save(request);

        ArgumentCaptor<SubjectPrompt> captor = ArgumentCaptor.forClass(SubjectPrompt.class);
        verify(repository).save(captor.capture());
        SubjectPrompt entity = captor.getValue();
        assertThat(entity.getSpecId()).isEqualTo(5L);
        assertThat(entity.getNameSpec()).isEqualTo("Physics");
        assertThat(entity.getSystemPrompt()).isEqualTo("Sys prompt");
        assertThat(entity.getVisionPrompt()).isEqualTo("Vis prompt");
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_existingPrompt_updatesAndReturns() {
        SubjectPrompt existing = buildPrompt(2L, "Old", "Old sys", "Old vis");
        when(repository.findById(2L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SubjectPromptResponse response = service.update(2L,
                new SubjectPromptRequest(2L, "New Name", "New sys", "New vis", null));

        assertThat(response.nameSpec()).isEqualTo("New Name");
        assertThat(response.systemPrompt()).isEqualTo("New sys");
        assertThat(response.visionPrompt()).isEqualTo("New vis");
    }

    @Test
    void update_notFound_throwsIllegalArgument() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L,
                new SubjectPromptRequest(99L, "X", "P", "V", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_callsRepositoryDeleteById() {
        service.delete(3L);
        verify(repository).deleteById(3L);
    }

    // ── findAll ───────────────────────────────────────────────────────────────

    @Test
    void findAll_returnsAllMapped() {
        when(repository.findAll()).thenReturn(List.of(
                buildPrompt(1L, "Math", "S1", "V1"),
                buildPrompt(2L, "Physics", "S2", "V2")
        ));

        List<SubjectPromptResponse> result = service.findAll();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SubjectPromptResponse::specId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void findAll_empty_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());
        assertThat(service.findAll()).isEmpty();
    }

    // ── findBySpecId ──────────────────────────────────────────────────────────

    @Test
    void findBySpecId_found_returnsOptionalWithEntity() {
        SubjectPrompt prompt = buildPrompt(7L, "Chemistry", "Sys", "Vis");
        when(repository.findById(7L)).thenReturn(Optional.of(prompt));

        Optional<SubjectPrompt> result = service.findBySpecId(7L);

        assertThat(result).isPresent().contains(prompt);
    }

    @Test
    void findBySpecId_notFound_returnsEmpty() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThat(service.findBySpecId(99L)).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SubjectPrompt buildPrompt(Long specId, String nameSpec, String systemPrompt, String visionPrompt) {
        SubjectPrompt p = new SubjectPrompt();
        p.setSpecId(specId);
        p.setNameSpec(nameSpec);
        p.setSystemPrompt(systemPrompt);
        p.setVisionPrompt(visionPrompt);
        return p;
    }
}
