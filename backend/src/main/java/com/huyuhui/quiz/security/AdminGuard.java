package com.huyuhui.quiz.security;

import com.huyuhui.quiz.common.BizException;
import com.huyuhui.quiz.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 权限校验工具
 *
 * <p>管理员接口需要角色为 ADMIN。这里独立成组件，而不是依赖
 * Spring Security 的 {@code @PreAuthorize}，是为了与项目现有的
 * 拦截器鉴权方式保持一致（本项目不用 Security 过滤器链做鉴权）。</p>
 */
@Component
public class AdminGuard {

    /**
     * 校验当前用户是管理员，否则抛出业务异常
     */
    public void requireAdmin(HttpServletRequest request) {
        User user = (User) request.getAttribute(AuthInterceptor.ATTR_USER);
        if (user == null) {
            throw new BizException(4010, "未登录");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException(4030, "需要管理员权限");
        }
    }
}