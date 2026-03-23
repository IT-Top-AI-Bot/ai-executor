package com.aquadev.ittopaiexecutor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "homework_extraction_image")
public class HomeworkExtractionImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "extraction_id", nullable = false)
    private HomeworkExtraction extraction;

    @Column(name = "image_data", nullable = false, columnDefinition = "BYTEA")
    private byte[] imageData;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;
}
