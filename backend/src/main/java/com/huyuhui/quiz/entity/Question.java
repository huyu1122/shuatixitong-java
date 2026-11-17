package com.huyuhui.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 题目表
 *
 * <p>options / answer / knowledge 三个字段在数据库中以 JSON 字符串存储，
 * 由实体上的 {@code @Convert} 或服务层做序列化转换。</p>
 */
@Entity
@Table(name = "quiz_question", indexes = {
        @Index(name = "uk_question_code", columnList = "code", unique = true),
        @Index(name = "idx_subject", columnList = "subject"),
        @Index(name = "idx_subject_chapter", columnList = "subject,chapter"),
        @Index(name = "idx_type", columnList = "type")
})
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 业务编码，如 m-001，用于导入去重 */
    @Column(nullable = false, length = 32, unique = true)
    private String code;

    /** 学科，如 高等数学 */
    @Column(nullable = false, length = 50)
    private String subject;

    /** 章节，如 极限与连续 */
    @Column(nullable = false, length = 50)
    private String chapter;

    /** 题型：single / multiple / judge / fill */
    @Column(nullable = false, length = 20)
    private String type;

    /** 难度：1 基础 / 2 中等 / 3 困难 */
    @Column(nullable = false)
    private Integer difficulty = 1;

    /** 题干，支持 LaTeX */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String stem;

    /** 选项 JSON 数组，判断题与填空题可为空 */
    @Column(columnDefinition = "TEXT")
    private String options;

    /** 正确答案：选项题存下标数组 JSON，填空题存答案文本 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    /** 解析 */
    @Column(columnDefinition = "TEXT")
    private String analysis;

    /** 知识点标签 JSON 数组 */
    @Column(columnDefinition = "TEXT")
    private String knowledge;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getChapter() { return chapter; }
    public void setChapter(String chapter) { this.chapter = chapter; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getDifficulty() { return difficulty; }
    public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

    public String getStem() { return stem; }
    public void setStem(String stem) { this.stem = stem; }

    public String getOptions() { return options; }
    public void setOptions(String options) { this.options = options; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public String getKnowledge() { return knowledge; }
    public void setKnowledge(String knowledge) { this.knowledge = knowledge; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}