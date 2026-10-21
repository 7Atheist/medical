package com.geriatric.assessment.config;

import com.geriatric.assessment.entity.SysParam;
import com.geriatric.assessment.repository.SysParamRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 后台管理鉴权拦截器：校验管理端访问令牌。
 *
 * <p>令牌优先级：</p>
 * <ol>
 *   <li>环境变量 ADMIN_TOKEN（application.yml 的 app.admin-token，生产推荐）</li>
 *   <li>数据库 sys_param 表 key=admin_token（后台可在线修改）</li>
 *   <li>都未配置则直接拒绝访问（不再硬编码默认值，避免上线后被弱口令爆破）</li>
 * </ol>
 * 请求头格式：Authorization: Bearer &lt;adminToken&gt;
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final SysParamRepository sysParamRepository;
    private final AppProperties props;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 解析请求里带的令牌
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7).trim();
        }

        // 2. 取配置的管理令牌：优先环境变量，其次数据库
        String adminToken = props.getAdminToken();
        if (adminToken == null || adminToken.isBlank()) {
            adminToken = sysParamRepository.findByParamKey("admin_token")
                    .map(SysParam::getParamValue).orElse(null);
        }

        // 3. 都未配置 → 拒绝（不允许硬编码弱口令上线）
        if (adminToken == null || adminToken.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"后台管理未配置令牌，请设置环境变量 ADMIN_TOKEN\",\"data\":null}");
            return false;
        }

        // 4. 校验
        if (token == null || !token.equals(adminToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"后台管理令牌错误\",\"data\":null}");
            return false;
        }
        return true;
    }
}
