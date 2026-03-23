package com.aquadev.ittopaiexecutor.repository;

import com.aquadev.ittopaiexecutor.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByApiSubjectId(Long apiSubjectId);
}
