package com.aquadev.aiexecutor.repository;

import com.aquadev.aiexecutor.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByApiSubjectId(Long apiSubjectId);
}
