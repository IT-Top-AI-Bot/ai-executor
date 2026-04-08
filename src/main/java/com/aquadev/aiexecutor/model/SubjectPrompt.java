package com.aquadev.aiexecutor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "subject_prompt",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_subject_prompt_user_spec",
                columnNames = {"telegram_user_id", "spec_id"}
        )
)
public class SubjectPrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "telegram_user_id", nullable = false)
    private Long telegramUserId;

    @Column(name = "spec_id", nullable = false)
    private Long specId;

    @Column(name = "name_spec", nullable = false)
    private String nameSpec;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "static_text", columnDefinition = "TEXT")
    private String staticText;
}
