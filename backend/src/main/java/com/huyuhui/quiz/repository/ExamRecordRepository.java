package com.huyuhui.quiz.repository;

import com.huyuhui.quiz.entity.ExamRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamRecordRepository extends JpaRepository<ExamRecord, Long> {
    List<ExamRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}