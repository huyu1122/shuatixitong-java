package com.huyuhui.quiz.controller;

import com.huyuhui.quiz.common.ApiResult;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.security.AuthInterceptor;
import com.huyuhui.quiz.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证接口：注册、登录、登出、获取当前用户
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record RegisterRequest(
            @NotBlank(message = "用户名不能为空")
            @Size(min = 3, max = 50, message = "用户名长度需在 3-50 之间")
            String username,
            @NotBlank(message = "密码不能为空")
            @Size(min = 6, max = 50, message = "密码长度需在 6-50 之间")
            String password,
            String nickname) {}

    public record LoginRequest(
            @NotBlank(message = "用户名不能为空") String username,
            @NotBlank(message = "密码不能为空") String password) {}

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@Valid @RequestBody RegisterRequest req,
                                                   HttpServletResponse response) {
        Map<String, Object> data = authService.register(req.username(), req.password(), req.nickname());
        setTokenCookie(response, (String) data.get("token"));
        return ApiResult.ok(data);
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@Valid @RequestBody LoginRequest req,
                                                HttpServletResponse response) {
        Map<String, Object> data = authService.login(req.username(), req.password());
        setTokenCookie(response, (String) data.get("token"));
        return ApiResult.ok(data);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = resolveToken(request);
        authService.logout(token);
        Cookie cookie = new Cookie("QUIZ_TOKEN", "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me(@RequestAttribute(AuthInterceptor.ATTR_USER) User user) {
        return ApiResult.ok(Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "nickname", user.getNickname() == null ? user.getUsername() : user.getNickname(),
                "role", user.getRole()
        ));
    }

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("QUIZ_TOKEN", token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(7 * 24 * 3600);
        response.addCookie(cookie);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        if (request.getCookies() != null) {
            for (Cookie c : request.getCookies()) {
                if ("QUIZ_TOKEN".equals(c.getName())) return c.getValue();
            }
        }
        return null;
    }
}