package com.aquadev.ittopaiexecutor.repository;

import com.aquadev.ittopaiexecutor.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByFio(String fio);
}
