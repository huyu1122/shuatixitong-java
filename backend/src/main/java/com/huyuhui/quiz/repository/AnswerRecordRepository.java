package com.huyuhui.quiz.repository;

import com.huyuhui.quiz.entity.AnswerRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, Long> {

    Optional<AnswerRecord> findByUserIdAndQuestionId(Long userId, Long questionId);

    List<AnswerRecord> findByUserId(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(r.correctCount), 0) FROM AnswerRecord r WHERE r.userId = :userId")
    long sumCorrectByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(r.attempts), 0) FROM AnswerRecord r WHERE r.userId = :userId")
    long sumAttemptsByUser(@Param("userId") Long userId);
}