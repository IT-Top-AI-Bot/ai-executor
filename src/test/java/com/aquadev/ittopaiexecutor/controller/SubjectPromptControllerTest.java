package com.aquadev.ittopaiexecutor.controller;

import com.aquadev.ittopaiexecutor.dto.SubjectPromptRequest;
import com.aquadev.ittopaiexecutor.dto.SubjectPromptResponse;
import com.aquadev.ittopaiexecutor.service.prompt.SubjectPromptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SubjectPromptControllerTest {

    @Mock
    SubjectPromptService service;

    @InjectMocks
    SubjectPromptController controller;

    MockMvc mockMvc;
    final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void getAll_returnsList() throws Exception {
        SubjectPromptResponse resp = new SubjectPromptResponse(1L, "Math", "System prompt", null, null);
        when(service.findAll()).thenReturn(List.of(resp));

        mockMvc.perform(get("/api/v1/subject-prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specId").value(1))
                .andExpect(jsonPath("$[0].nameSpec").value("Math"));
    }

    @Test
    void getAll_returnsEmptyList() throws Exception {
        when(service.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/subject-prompts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void create_returnsCreated_whenValidRequest() throws Exception {
        SubjectPromptRequest request = new SubjectPromptRequest(1L, "Math", "System prompt", null, null);
        SubjectPromptResponse resp = new SubjectPromptResponse(1L, "Math", "System prompt", null, null);
        when(service.save(any())).thenReturn(resp);

        mockMvc.perform(post("/api/v1/subject-prompts")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.specId").value(1))
                .andExpect(jsonPath("$.systemPrompt").value("System prompt"));
    }

    @Test
    void update_returnsOk_whenValidRequest() throws Exception {
        SubjectPromptRequest request = new SubjectPromptRequest(1L, "Math Updated", "Updated prompt", null, null);
        SubjectPromptResponse resp = new SubjectPromptResponse(1L, "Math Updated", "Updated prompt", null, null);
        when(service.upsert(eq(1L), any())).thenReturn(resp);

        mockMvc.perform(put("/api/v1/subject-prompts/{specId}", 1L)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameSpec").value("Math Updated"))
                .andExpect(jsonPath("$.systemPrompt").value("Updated prompt"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/api/v1/subject-prompts/{specId}", 1L))
                .andExpect(status().isNoContent());
    }
}
