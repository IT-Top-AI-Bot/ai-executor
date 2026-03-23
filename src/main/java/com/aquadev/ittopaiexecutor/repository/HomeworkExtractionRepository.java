package com.aquadev.ittopaiexecutor.repository;

import com.aquadev.ittopaiexecutor.entity.HomeworkExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeworkExtractionRepository extends JpaRepository<HomeworkExtraction, Long> {

    Optional<HomeworkExtraction> findByHomeworkId(Long homeworkId);
}
