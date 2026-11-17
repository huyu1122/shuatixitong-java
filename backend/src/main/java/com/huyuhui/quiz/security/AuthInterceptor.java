package com.huyuhui.quiz.security;

import com.huyuhui.quiz.entity.Session;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.repository.SessionRepository;
import com.huyuhui.quiz.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Token 鉴权拦截器
 *
 * <p>从请求头 {@code Authorization: Bearer <token>} 或 Cookie 中读取 token，
 * 校验有效性后把当前用户放入 request attribute，供 Controller 使用。</p>
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** request attribute 中存放当前登录用户的 key */
    public static final String ATTR_USER = "currentUser";

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public AuthInterceptor(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 放行预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || token.isBlank()) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        Optional<Session> sessionOpt = sessionRepository.findByToken(token);
        if (sessionOpt.isEmpty()) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        Session session = sessionOpt.get();
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepository.delete(session);
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            writeUnauthorized(response, "用户不存在");
            return false;
        }

        request.setAttribute(ATTR_USER, userOpt.get());
        return true;
    }

    /** 优先读 Authorization 头，其次读 Cookie */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        if (request.getCookies() != null) {
            for (var c : request.getCookies()) {
                if ("QUIZ_TOKEN".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":4010,\"message\":\"" + message + "\",\"data\":null}");
    }
}