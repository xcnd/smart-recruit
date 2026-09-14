package com.smartrecruit.recruitment.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类，从 Spring Security 上下文获取当前用户信息。
 * 与 Talent 和 Offer 模块的做法保持一致。
 *
 * @since 1.0.0
 */
public final class UserContextUtil {

    private UserContextUtil() {
    }

    /**
     * 获取当前登录用户 ID。
     *
     * @return 用户 ID，未认证时返回 {@code null}
     */
    public static Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            return null;
        }
        String credentials = auth.getCredentials().toString();
        if (credentials.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(credentials);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户 ID，未认证时返回默认值。
     *
     * @param defaultValue 未认证时的默认值
     * @return 用户 ID
     */
    public static Long getUserIdOrDefault(Long defaultValue) {
        Long userId = getUserId();
        return userId != null ? userId : defaultValue;
    }

    /**
     * 获取当前登录用户名。
     *
     * @return 用户名，未认证或匿名时返回 {@code null}
     */
    public static String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            return null;
        }
        String username = auth.getPrincipal().toString();
        return username == null || username.isBlank() || "anonymousUser".equals(username)
                ? null : username;
    }
}
