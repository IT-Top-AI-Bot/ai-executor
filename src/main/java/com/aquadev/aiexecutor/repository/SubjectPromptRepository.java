package com.aquadev.aiexecutor.repository;

import com.aquadev.aiexecutor.model.SubjectPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectPromptRepository extends JpaRepository<SubjectPrompt, Long> {

    boolean existsByTelegramUserIdAndSpecId(Long telegramUserId, Long specId);

    Optional<SubjectPrompt> findByTelegramUserIdAndSpecId(Long telegramUserId, Long specId);

    List<SubjectPrompt> findAllByTelegramUserId(Long telegramUserId);

    void deleteByTelegramUserIdAndSpecId(Long telegramUserId, Long specId);
}
