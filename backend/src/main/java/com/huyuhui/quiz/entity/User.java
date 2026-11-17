package com.huyuhui.quiz.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Entity
@Table(name = "sys_user", indexes = {
        @Index(name = "uk_username", columnList = "username", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录用户名，唯一 */
    @Column(nullable = false, length = 50, unique = true)
    private String username;

    /** BCrypt 加密后的密码 */
    @Column(nullable = false, length = 100)
    private String password;

    /** 昵称 */
    @Column(length = 50)
    private String nickname;

    /** 角色：STUDENT / ADMIN */
    @Column(nullable = false, length = 20)
    private String role = "STUDENT";

    /** 连续登录失败次数（防爆破） */
    @Column(nullable = false)
    private Integer failedAttempts = 0;

    /** 锁定到期时间，null 表示未锁定 */
    private LocalDateTime lockedUntil;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}