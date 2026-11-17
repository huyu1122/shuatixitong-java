package com.huyuhui.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 错题本 / 收藏表
 *
 * <p>用 type 字段区分「错题」与「收藏」，避免建两张结构相同的表。
 * type = WRONG 表示错题，type = FAVORITE 表示收藏。</p>
 */
@Entity
@Table(name = "quiz_mark", indexes = {
        @Index(name = "uk_user_question_type", columnList = "userId,questionId,type", unique = true),
        @Index(name = "idx_user_type", columnList = "userId,type")
})
public class QuestionMark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long questionId;

    /** WRONG / FAVORITE */
    @Column(nullable = false, length = 20)
    private String type;

    /** 是否已订正（仅错题有效） */
    @Column(nullable = false)
    private Boolean resolved = false;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getResolved() { return resolved; }
    public void setResolved(Boolean resolved) { this.resolved = resolved; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}