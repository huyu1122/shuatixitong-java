package com.huyuhui.quiz.repository;

import com.huyuhui.quiz.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    boolean existsByCode(String code);

    /**
     * 按学科 / 章节 / 题型 / 难度 / 关键词组合筛选，参数为 null 时忽略该条件。
     */
    @Query("""
            SELECT q FROM Question q
            WHERE (:subject IS NULL OR q.subject = :subject)
              AND (:chapter IS NULL OR q.chapter = :chapter)
              AND (:type IS NULL OR q.type = :type)
              AND (:difficulty IS NULL OR q.difficulty = :difficulty)
              AND (:keyword IS NULL OR q.stem LIKE %:keyword%)
            """)
    Page<Question> search(@Param("subject") String subject,
                          @Param("chapter") String chapter,
                          @Param("type") String type,
                          @Param("difficulty") Integer difficulty,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    @Query("SELECT DISTINCT q.subject FROM Question q ORDER BY q.subject")
    List<String> findDistinctSubjects();

    @Query("SELECT DISTINCT q.chapter FROM Question q WHERE q.subject = :subject ORDER BY q.chapter")
    List<String> findDistinctChaptersBySubject(@Param("subject") String subject);

    List<Question> findBySubjectOrderByIdAsc(String subject);

    List<Question> findByIdIn(List<Long> ids);
}