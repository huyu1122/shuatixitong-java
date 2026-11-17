package com.huyuhui.quiz.repository;

import com.huyuhui.quiz.entity.QuestionMark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionMarkRepository extends JpaRepository<QuestionMark, Long> {

    Optional<QuestionMark> findByUserIdAndQuestionIdAndType(Long userId, Long questionId, String type);

    List<QuestionMark> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type);

    long countByUserIdAndTypeAndResolved(Long userId, String type, Boolean resolved);

    void deleteByUserIdAndQuestionIdAndType(Long userId, Long questionId, String type);
}