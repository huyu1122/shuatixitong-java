package com.huyuhui.quiz.controller;

import com.huyuhui.quiz.common.ApiResult;
import com.huyuhui.quiz.entity.Question;
import com.huyuhui.quiz.entity.User;
import com.huyuhui.quiz.security.AdminGuard;
import com.huyuhui.quiz.security.AuthInterceptor;
import com.huyuhui.quiz.service.AdminService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理接口（需管理员角色）
 *
 * <p>所有方法入口都调用 {@link AdminGuard#requireAdmin} 校验权限。</p>
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final AdminGuard adminGuard;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminService adminService, AdminGuard adminGuard,
                           PasswordEncoder passwordEncoder) {
        this.adminService = adminService;
        this.adminGuard = adminGuard;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== 总览 ====================

    @GetMapping("/dashboard")
    public ApiResult<Map<String, Object>> dashboard(HttpServletRequest req) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.dashboard());
    }

    // ==================== 题目管理 ====================

    @GetMapping("/questions")
    public ApiResult<Map<String, Object>> listQuestions(
            HttpServletRequest req,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        adminGuard.requireAdmin(req);
        Page<Question> result = adminService.listQuestions(subject, keyword, page, size);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("content", result.getContent());
        data.put("total", result.getTotalElements());
        data.put("page", result.getNumber());
        data.put("size", result.getSize());
        data.put("totalPages", result.getTotalPages());
        return ApiResult.ok(data);
    }

    @GetMapping("/questions/stats")
    public ApiResult<Map<String, Object>> questionStats(HttpServletRequest req) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.questionStats());
    }

    @PostMapping("/questions")
    public ApiResult<Question> createQuestion(HttpServletRequest req,
                                              @RequestBody Map<String, Object> body) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.createQuestion(body));
    }

    @PutMapping("/questions/{id}")
    public ApiResult<Question> updateQuestion(HttpServletRequest req,
                                              @PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.updateQuestion(id, body));
    }

    @DeleteMapping("/questions/{id}")
    public ApiResult<Void> deleteQuestion(HttpServletRequest req, @PathVariable Long id) {
        adminGuard.requireAdmin(req);
        adminService.deleteQuestion(id);
        return ApiResult.ok();
    }

    @PostMapping("/questions/batch-delete")
    public ApiResult<Map<String, Object>> batchDelete(HttpServletRequest req,
                                                      @RequestBody List<Long> ids) {
        adminGuard.requireAdmin(req);
        int deleted = adminService.batchDelete(ids);
        return ApiResult.ok(Map.of("deleted", deleted));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public ApiResult<List<Map<String, Object>>> listUsers(HttpServletRequest req,
                                                          @RequestParam(required = false) String keyword) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.listUsers(keyword));
    }

    public record RoleRequest(String role) {}
    public record PasswordRequest(String password) {}

    @PutMapping("/users/{id}/role")
    public ApiResult<User> setRole(HttpServletRequest req,
                                   @PathVariable Long id,
                                   @RequestBody RoleRequest body) {
        adminGuard.requireAdmin(req);
        User operator = (User) req.getAttribute(AuthInterceptor.ATTR_USER);
        return ApiResult.ok(adminService.setRole(id, body.role(), operator.getId()));
    }

    @PostMapping("/users/{id}/unlock")
    public ApiResult<User> unlock(HttpServletRequest req, @PathVariable Long id) {
        adminGuard.requireAdmin(req);
        return ApiResult.ok(adminService.unlock(id));
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResult<Void> resetPassword(HttpServletRequest req,
                                         @PathVariable Long id,
                                         @RequestBody PasswordRequest body) {
        adminGuard.requireAdmin(req);
        adminService.resetPassword(id, body.password(), passwordEncoder);
        return ApiResult.ok();
    }

    @DeleteMapping("/users/{id}")
    public ApiResult<Void> deleteUser(HttpServletRequest req, @PathVariable Long id) {
        adminGuard.requireAdmin(req);
        User operator = (User) req.getAttribute(AuthInterceptor.ATTR_USER);
        adminService.deleteUser(id, operator.getId());
        return ApiResult.ok();
    }
}