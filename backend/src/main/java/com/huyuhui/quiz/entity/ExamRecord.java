package com.huyuhui.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 模拟考试记录表
 */
@Entity
@Table(name = "quiz_exam", indexes = {
        @Index(name = "idx_exam_user", columnList = "userId")
})
public class ExamRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 考试范围描述，如「高等数学 · 模拟考」 */
    @Column(nullable = false, length = 100)
    private String scope;

    /** 题目总数 */
    @Column(nullable = false)
    private Integer total = 0;

    /** 答对题数 */
    @Column(nullable = false)
    private Integer correctCount = 0;

    /** 得分（百分制） */
    @Column(nullable = false)
    private Integer score = 0;

    /** 用时（秒） */
    @Column(nullable = false)
    private Integer durationSeconds = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public Integer getCorrectCount() { return correctCount; }
    public void setCorrectCount(Integer correctCount) { this.correctCount = correctCount; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}