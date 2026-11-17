package com.huyuhui.quiz.bootstrap;

import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 管理员账号初始化
 *
 * <p>首次启动时若不存在管理员账号，则自动创建一个，
 * 避免部署后无法进入后台（后台需要 ADMIN 角色，而注册接口只创建 STUDENT）。</p>
 *
 * <p>账号密码可通过环境变量覆盖：
 * {@code QUIZ_ADMIN_USERNAME}、{@code QUIZ_ADMIN_PASSWORD}。</p>
 */
@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${quiz.admin.username:admin}")
    private String adminUsername;

    @Value("${quiz.admin.password:admin123}")
    private String adminPassword;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 已存在任意管理员则跳过
        boolean hasAdmin = userRepository.findAll().stream()
                .anyMatch(u -> "ADMIN".equals(u.getRole()));
        if (hasAdmin) {
            log.info("已存在管理员账号，跳过初始化");
            return;
        }

        // 若该用户名已被普通用户占用，则提升为管理员
        var existing = userRepository.findByUsername(adminUsername);
        if (existing.isPresent()) {
            User user = existing.get();
            user.setRole("ADMIN");
            userRepository.save(user);
            log.info("已将已有账号 {} 提升为管理员", adminUsername);
            return;
        }

        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname("系统管理员");
        admin.setRole("ADMIN");
        userRepository.save(admin);

        log.info("已创建默认管理员账号：{} / {}（请尽快修改密码）", adminUsername, adminPassword);
    }
}