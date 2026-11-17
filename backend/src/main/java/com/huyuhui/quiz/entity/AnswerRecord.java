package com.huyuhui.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 答题记录表（每用户每题目一行，累计统计）
 */
@Entity
@Table(name = "quiz_record", indexes = {
        @Index(name = "uk_user_question", columnList = "userId,questionId", unique = true)
})
public class AnswerRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long questionId;

    /** 作答总次数 */
    @Column(nullable = false)
    private Integer attempts = 0;

    /** 答对次数 */
    @Column(nullable = false)
    private Integer correctCount = 0;

    /** 最近一次是否答对 */
    @Column(nullable = false)
    private Boolean lastCorrect = false;

    /** 最近一次提交的答案 */
    @Column(columnDefinition = "TEXT")
    private String lastAnswer;

    @Column(nullable = false)
    private LocalDateTime lastAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }

    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }

    public Boolean getLastCorrect() { return lastCorrect; }
    public void setLastCorrect(Boolean lastCorrect) { this.lastCorrect = lastCorrect; }

    public String getLastAnswer() { return lastAnswer; }
    public void setLastAnswer(String lastAnswer) { this.lastAnswer = lastAnswer; }

    public LocalDateTime getLastAt() { return lastAt; }
    public void setLastAt(LocalDateTime lastAt) { this.lastAt = lastAt; }
}