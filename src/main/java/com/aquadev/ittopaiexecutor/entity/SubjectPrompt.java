package com.aquadev.ittopaiexecutor.entity;

import jakarta.persistence.*;
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

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "vision_prompt", columnDefinition = "TEXT")
    private String visionPrompt;
}
