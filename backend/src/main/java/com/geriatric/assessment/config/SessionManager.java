package com.geriatric.assessment.config;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录会话管理器（内存版）。
 *
 * <p>登录成功生成随机 token，建立 token → userId 映射并记录最近访问时间，
 * 超过 {@code sessionExpireHours} 自动失效。</p>
 *
 * <p>说明：单实例部署可直接使用；若需多实例/水平扩展，请将本实现替换为
 * Redis 存储（键 `session:{token}`，TTL=过期时长），接口保持不变。</p>
 */
@Component
public class SessionManager {

    /** token → (userId, 最近访问时间戳) */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    private int expireHours = 72;

    /** 启动时由调用方注入过期时长 */
    public void init(int expireHours) {
        this.expireHours = expireHours;
    }

    /** 创建会话 */
    public String create(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, new Session(userId, System.currentTimeMillis()));
        return token;
    }

    /**
     * 校验并刷新会话。
     *
     * @return 有效返回 userId，否则返回 null
     */
    public Long validate(String token) {
        if (token == null) {
            return null;
        }
        Session s = sessions.get(token);
        if (s == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (now - s.lastAccess > expireHours * 3600L * 1000L) {
            sessions.remove(token);
            return null;
        }
        s.lastAccess = now; // 滑动续期
        return s.userId;
    }

    /** 登出 */
    public void remove(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }

    private static class Session {
        final Long userId;
        volatile long lastAccess;

        Session(Long userId, long lastAccess) {
            this.userId = userId;
            this.lastAccess = lastAccess;
        }
    }
}
