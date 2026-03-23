package com.aquadev.ittopaiexecutor.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "homework_extraction")
public class HomeworkExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homework_id", nullable = false, unique = true)
    private Long homeworkId;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @OneToMany(mappedBy = "extraction", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private List<HomeworkExtractionImage> images = new ArrayList<>();
}
