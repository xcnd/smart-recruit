package com.smartrecruit.common.util;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Callable;

/**
 * 基于 ScopedValue 的上下文持有者，用于在虚拟线程中跨线程边界传播请求作用域数据
 * （用户信息、追踪 ID、租户 ID）。
 *
 * <p>Java 25 的 {@link ScopedValue} 是 {@link ThreadLocal} 的继任者，
 * 用于结构化并发和虚拟线程场景。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 在请求入口点设置上下文
 * ScopedValueContext.set("userId", 12345L);
 * ScopedValueContext.set("traceId", UUID.randomUUID().toString());
 *
 * // 在作用域上下文中运行
 * ScopedValueContext.run(() -> {
 *     Long userId = ScopedValueContext.getUserId();
 *     String traceId = ScopedValueContext.getTraceId();
 *     // ... 业务逻辑
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ScopedValueContext {

    /** 持有上下文映射的 ScopedValue。 */
    private static final ScopedValue<Map<String, Object>> CONTEXT = ScopedValue.newInstance();

    /** 键常量。 */
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_TENANT_ID = "tenantId";
    public static final String KEY_TRACE_ID = "traceId";
    public static final String KEY_REQUEST_URI = "requestUri";
    public static final String KEY_CLIENT_IP = "clientIp";

    private ScopedValueContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ================================================================
    // 上下文生命周期
    // ================================================================

    /**
     * 从源映射创建上下文映射，运行任务，然后清理。
     */
    public static <T> T run(Map<String, Object> context, Callable<T> task) throws Exception {
        Map<String, Object> ctx = new ConcurrentHashMap<>(context);
        return ScopedValue.where(CONTEXT, ctx).call(() -> task.call());
    }

    /**
     * 在当前上下文（或空上下文）中运行一个 Runnable。
     */
    public static void run(Runnable task) {
        Map<String, Object> ctx = CONTEXT.isBound() ? CONTEXT.get() : new ConcurrentHashMap<>();
        ScopedValue.where(CONTEXT, ctx).run(task);
    }

    // ================================================================
    // 设置器
    // ================================================================

    public static void set(String key, Object value) {
        if (CONTEXT.isBound()) {
            CONTEXT.get().put(key, value);
        }
    }

    public static void setUserId(Object userId) {
        set(KEY_USER_ID, userId);
    }

    public static void setUsername(String username) {
        set(KEY_USERNAME, username);
    }

    public static void setTenantId(Object tenantId) {
        set(KEY_TENANT_ID, tenantId);
    }

    public static void setTraceId(String traceId) {
        set(KEY_TRACE_ID, traceId);
    }

    public static void setRequestUri(String uri) {
        set(KEY_REQUEST_URI, uri);
    }

    public static void setClientIp(String ip) {
        set(KEY_CLIENT_IP, ip);
    }

    // ================================================================
    // 获取器
    // ================================================================

    public static Optional<Object> get(String key) {
        if (!CONTEXT.isBound()) return Optional.empty();
        return Optional.ofNullable(CONTEXT.get().get(key));
    }

    public static Long getUserId() {
        return get(KEY_USER_ID).map(v -> v instanceof Number n ? n.longValue() : null).orElse(null);
    }

    public static String getUsername() {
        return get(KEY_USERNAME).map(Object::toString).orElse(null);
    }

    public static String getTenantId() {
        return get(KEY_TENANT_ID).map(Object::toString).orElse(null);
    }

    public static String getTraceId() {
        return get(KEY_TRACE_ID).map(Object::toString).orElse(null);
    }

    public static String getRequestUri() {
        return get(KEY_REQUEST_URI).map(Object::toString).orElse(null);
    }

    public static String getClientIp() {
        return get(KEY_CLIENT_IP).map(Object::toString).orElse(null);
    }

    // ================================================================
    // 上下文检查
    // ================================================================

    /**
     * 如果 ScopedValue 上下文已绑定到当前线程/载体，则返回 {@code true}。
     */
    public static boolean isBound() {
        return CONTEXT.isBound();
    }

    /**
     * 从当前上下文中移除所有条目（不解绑）。
     */
    public static void clear() {
        if (CONTEXT.isBound()) {
            CONTEXT.get().clear();
        }
    }
}
