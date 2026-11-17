package com.huyuhui.quiz.service;

import com.huyuhui.quiz.common.BizException;
import com.huyuhui.quiz.entity.Session;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.repository.SessionRepository;
import com.huyuhui.quiz.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * 认证服务：注册、登录、登出
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${quiz.session-ttl-hours:168}")
    private long sessionTtlHours;

    @Value("${quiz.login-max-failures:5}")
    private int maxFailures;

    @Value("${quiz.login-lock-minutes:10}")
    private int lockMinutes;

    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository userRepository,
                       SessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册
     */
    @Transactional
    public Map<String, Object> register(String username, String rawPassword, String nickname) {
        if (username == null || username.trim().length() < 3) {
            throw new BizException("用户名至少 3 个字符");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new BizException("密码至少 6 个字符");
        }
        String name = username.trim();
        if (userRepository.existsByUsername(name)) {
            throw new BizException("用户名已被注册");
        }

        User user = new User();
        user.setUsername(name);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname((nickname == null || nickname.isBlank()) ? name : nickname.trim());
        user.setRole("STUDENT");
        userRepository.save(user);

        return issueToken(user);
    }

    /**
     * 登录（含防爆破锁定）
     */
    @Transactional
    public Map<String, Object> login(String username, String rawPassword) {
        String name = (username == null) ? "" : username.trim();
        User user = userRepository.findByUsername(name)
                .orElseThrow(() -> new BizException("用户名或密码错误"));

        // 锁定检查
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BizException("账号已锁定，请 " + lockMinutes + " 分钟后再试");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            int failures = user.getFailedAttempts() + 1;
            user.setFailedAttempts(failures);
            if (failures >= maxFailures) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
                user.setFailedAttempts(0);
            }
            userRepository.save(user);
            throw new BizException("用户名或密码错误");
        }

        // 登录成功：重置失败计数
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return issueToken(user);
    }

    /**
     * 登出：删除当前 token
     */
    @Transactional
    public void logout(String token) {
        if (token != null) {
            sessionRepository.findByToken(token).ifPresent(sessionRepository::delete);
        }
    }

    /** 生成 32 字节随机 token 并落库 */
    private Map<String, Object> issueToken(User user) {
        byte[] buf = new byte[32];
        random.nextBytes(buf);
        String token = HexFormat.of().formatHex(buf);

        Session session = new Session();
        session.setToken(token);
        session.setUserId(user.getId());
        session.setExpiresAt(LocalDateTime.now().plusHours(sessionTtlHours));
        sessionRepository.save(session);

        return Map.of(
                "token", token,
                "expiresAt", session.getExpiresAt().toString(),
                "user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname(),
                        "role", user.getRole()
                )
        );
    }
}