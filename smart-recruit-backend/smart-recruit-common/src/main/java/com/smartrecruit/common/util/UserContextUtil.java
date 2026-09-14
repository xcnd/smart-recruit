package com.smartrecruit.common.util;

import com.smartrecruit.common.exception.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类。
 *
 * <p>基于 ThreadLocal 维护当前请求的用户上下文信息，
 * 包括用户 ID、用户名和 Token。在请求处理完成后必须调用 {@link #clear()} 清除上下文，
 * 避免内存泄漏和线程池复用时数据串扰。
 *
 * <p>典型使用场景：
 * <ul>
 *   <li>Controller/Service 层获取当前登录用户，无需从 HttpServletRequest 解析</li>
 *   <li>从网关注入的 X-User-Id 请求头读取用户 ID</li>
 *   <li>操作日志切面记录操作用户</li>
 * </ul>
 *
 * @author xdh
 * @since 1.0.0
 */
public final class UserContextUtil {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    private UserContextUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 获取当前登录用户 ID（系统统一入口）。
     *
     * <p>优先级：ThreadLocal（由各服务 {@code JwtAuthenticationFilter} 解析后设置）
     * → Spring Security 上下文（各服务过滤器将用户 ID 存入 credentials）。</p>
     *
     * @return 用户 ID，未登录或解析失败时返回 {@code null}
     */
    public static Long getCurrentUserId() {
        Long threadLocalUserId = getUserId();
        if (threadLocalUserId != null) {
            return threadLocalUserId;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getCredentials() == null) {
            return null;
        }
        String credentials = auth.getCredentials().toString();
        if (credentials.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(credentials);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取当前登录用户 ID，未登录或凭证异常时抛出 {@link AuthenticationException}。
     *
     * <p>优先级：ThreadLocal（由各服务 {@code JwtAuthenticationFilter} 解析后设置）
     * → Spring Security 上下文（各服务过滤器将用户 ID 存入 credentials）。</p>
     *
     * @return 当前用户 ID
     * @throws AuthenticationException 未登录、凭证缺失或凭证无效时抛出
     */
    public static Long getCurrentUserIdOrThrow() {
        Long threadLocalUserId = getUserId();
        if (threadLocalUserId != null) {
            return threadLocalUserId;
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationException("未登录");
        }
        Object credentials = auth.getCredentials();
        if (credentials == null || credentials.toString().isEmpty()) {
            throw new AuthenticationException("用户凭证缺失");
        }
        try {
            return Long.parseLong(credentials.toString());
        } catch (NumberFormatException e) {
            throw new AuthenticationException("用户凭证无效");
        }
    }

    public static void setUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    public static String getUsername() {
        return USERNAME_HOLDER.get();
    }

    public static void setToken(String token) {
        TOKEN_HOLDER.set(token);
    }

    public static String getToken() {
        return TOKEN_HOLDER.get();
    }

    public static void setRole(String role) {
        ROLE_HOLDER.set(role);
    }

    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    /**
     * 从网关注入的 X-User-Id 请求头中获取用户 ID。
     *
     * <p>优先从 ThreadLocal 获取（由 JwtAuthenticationFilter 在解析 Token 后设置），
     * 降级后从请求头读取（由网关 AuthFilter 注入）。
     *
     * @param request HTTP 请求
     * @return 用户 ID，未获取到时返回 null
     */
    public static Long getUserIdFromHeader(HttpServletRequest request) {
        Long threadLocalUserId = getUserId();
        if (threadLocalUserId != null) {
            return threadLocalUserId;
        }
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isLoggedIn() {
        return getUserId() != null;
    }

    /**
     * 清除当前线程的用户上下文。
     *
     * <p>必须在请求处理完成后调用（通常在过滤器的 finally 块中），
     * 防止 ThreadLocal 内存泄漏和线程池数据串扰。
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
        TOKEN_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
