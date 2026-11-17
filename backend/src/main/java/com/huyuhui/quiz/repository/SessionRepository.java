package com.huyuhui.quiz.repository;

import com.huyuhui.quiz.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {
    Optional<Session> findByToken(String token);
    long deleteByExpiresAtBefore(LocalDateTime time);
    void deleteByUserId(Long userId);
}