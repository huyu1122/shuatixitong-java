-- ============================================================
--  学而刷题 · 大学生在线刷题系统 —— MySQL 建表脚本
--  数据库：quiz_system
--  字符集：utf8mb4（支持 emoji 与全部中文）
--  引擎：InnoDB（支持事务与外键）
--
--  使用方式：
--    mysql -u root -p < schema.sql
-- ============================================================

DROP DATABASE IF EXISTS quiz_system;
CREATE DATABASE quiz_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE quiz_system;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
CREATE TABLE sys_user (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username        VARCHAR(50)  NOT NULL                COMMENT '登录用户名',
    password        VARCHAR(100) NOT NULL                COMMENT 'BCrypt 加密后的密码',
    nickname        VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
    role            VARCHAR(20)  NOT NULL DEFAULT 'STUDENT' COMMENT '角色：STUDENT/ADMIN',
    failed_attempts INT          NOT NULL DEFAULT 0      COMMENT '连续登录失败次数',
    locked_until    DATETIME     DEFAULT NULL            COMMENT '锁定到期时间',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. 会话表
-- ------------------------------------------------------------
CREATE TABLE sys_session (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    token      VARCHAR(64) NOT NULL                COMMENT '会话 token',
    user_id    BIGINT      NOT NULL                COMMENT '所属用户',
    expires_at DATETIME    NOT NULL                COMMENT '过期时间',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    KEY idx_user (user_id),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表';

-- ------------------------------------------------------------
-- 3. 题目表
--    options / answer / knowledge 以 JSON 字符串存储，
--    便于兼容单选题、多选题、判断题、填空题四种题型。
-- ------------------------------------------------------------
CREATE TABLE quiz_question (
    id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    code       VARCHAR(32) NOT NULL                COMMENT '业务编码，如 m-001',
    subject    VARCHAR(50) NOT NULL                COMMENT '学科',
    chapter    VARCHAR(50) NOT NULL                COMMENT '章节',
    type       VARCHAR(20) NOT NULL                COMMENT '题型：single/multiple/judge/fill',
    difficulty INT         NOT NULL DEFAULT 1      COMMENT '难度：1基础 2中等 3困难',
    stem       TEXT        NOT NULL                COMMENT '题干（支持 LaTeX）',
    options    TEXT        DEFAULT NULL            COMMENT '选项 JSON 数组',
    answer     TEXT        NOT NULL                COMMENT '正确答案：选项题为下标数组，填空题为文本',
    analysis   TEXT        DEFAULT NULL            COMMENT '解析',
    knowledge  TEXT        DEFAULT NULL            COMMENT '知识点标签 JSON 数组',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_code (code),
    KEY idx_subject (subject),
    KEY idx_subject_chapter (subject, chapter),
    KEY idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ------------------------------------------------------------
-- 4. 答题记录表（每用户每题目一行，累计统计）
-- ------------------------------------------------------------
CREATE TABLE quiz_record (
    id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id       BIGINT   NOT NULL                COMMENT '用户',
    question_id   BIGINT   NOT NULL                COMMENT '题目',
    attempts      INT      NOT NULL DEFAULT 0      COMMENT '作答总次数',
    correct_count INT      NOT NULL DEFAULT 0      COMMENT '答对次数',
    last_correct  TINYINT(1) NOT NULL DEFAULT 0    COMMENT '最近一次是否答对',
    last_answer   TEXT     DEFAULT NULL            COMMENT '最近一次提交的答案',
    last_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近作答时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question (user_id, question_id),
    CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_record_question FOREIGN KEY (question_id) REFERENCES quiz_question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- ------------------------------------------------------------
-- 5. 错题本 / 收藏表
--    type 区分 WRONG（错题）与 FAVORITE（收藏）
-- ------------------------------------------------------------
CREATE TABLE quiz_mark (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL                COMMENT '用户',
    question_id BIGINT      NOT NULL                COMMENT '题目',
    type        VARCHAR(20) NOT NULL                COMMENT '类型：WRONG/FAVORITE',
    resolved    TINYINT(1)  NOT NULL DEFAULT 0      COMMENT '是否已订正（仅错题）',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_question_type (user_id, question_id, type),
    KEY idx_user_type (user_id, type),
    CONSTRAINT fk_mark_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_mark_question FOREIGN KEY (question_id) REFERENCES quiz_question (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本与收藏表';

-- ------------------------------------------------------------
-- 6. 模拟考试记录表
-- ------------------------------------------------------------
CREATE TABLE quiz_exam (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT       NOT NULL                COMMENT '用户',
    scope            VARCHAR(100) NOT NULL                COMMENT '考试范围',
    total            INT          NOT NULL DEFAULT 0      COMMENT '题目总数',
    correct_count    INT          NOT NULL DEFAULT 0      COMMENT '答对题数',
    score            INT          NOT NULL DEFAULT 0      COMMENT '得分（百分制）',
    duration_seconds INT          NOT NULL DEFAULT 0      COMMENT '用时（秒）',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交卷时间',
    PRIMARY KEY (id),
    KEY idx_exam_user (user_id),
    CONSTRAINT fk_exam_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模拟考试记录表';