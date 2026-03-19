package com.aquadev.ittopaiexecutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subject_prompt")
public class SubjectPrompt {

    @Id
    @Column(name = "spec_id")
    private Long specId;

    @Column(name = "name_spec", nullable = false)
    private String nameSpec;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "vision_prompt", columnDefinition = "TEXT")
    private String visionPrompt;

    @Column(name = "static_text", columnDefinition = "TEXT")
    private String staticText;
}
