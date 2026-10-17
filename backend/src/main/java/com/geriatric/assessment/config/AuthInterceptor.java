package com.geriatric.assessment.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录鉴权拦截器：除登录、后台管理、静态资源等白名单外，
 * 其余接口必须携带合法 token（请求头 Authorization: Bearer xxx）。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    /** 会话持有者：request attribute 中的 userId */
    public static final String ATTR_USER_ID = "loginUserId";
    public static final String ATTR_ADMIN = "isAdmin";

    private final SessionManager sessionManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        // 使用 servletPath（不含 context-path /api）
        String path = request.getServletPath();

        // 后台管理走独立令牌校验（见 AdminAuthInterceptor），此处跳过
        if (path.startsWith("/admin/")) {
            return true;
        }
        // 白名单
        if (path.startsWith("/auth/login") || path.startsWith("/health")) {
            return true;
        }

        String token = extractToken(request);
        Long userId = sessionManager.validate(token);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已失效，请重新登录\",\"data\":null}");
            return false;
        }
        request.setAttribute(ATTR_USER_ID, userId);
        return true;
    }

    /** 从请求头提取 Bearer token */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        String token = request.getParameter("token");
        return token == null || token.isBlank() ? null : token;
    }
}
