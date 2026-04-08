package com.aquadev.aiexecutor.repository;

import com.aquadev.aiexecutor.model.HomeworkExtraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HomeworkExtractionRepository extends JpaRepository<HomeworkExtraction, Long> {

    Optional<HomeworkExtraction> findByHomeworkId(Long homeworkId);
}
