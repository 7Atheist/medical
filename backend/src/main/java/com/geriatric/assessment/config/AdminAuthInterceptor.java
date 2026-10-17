package com.geriatric.assessment.config;

import com.geriatric.assessment.entity.SysParam;
import com.geriatric.assessment.repository.SysParamRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理鉴权拦截器：通过系统参数 admin_token 校验管理端访问。
 * 请求头 Authorization: Bearer adminToken 匹配即视为管理员，否则 401。
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final SysParamRepository sysParamRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }
        String adminToken = sysParamRepository.findByParamKey("admin_token")
                .map(SysParam::getParamValue).orElse("admin@2026");
        if (token == null || adminToken == null || !token.equals(adminToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"后台管理令牌错误\",\"data\":null}");
            return false;
        }
        return true;
    }
}
