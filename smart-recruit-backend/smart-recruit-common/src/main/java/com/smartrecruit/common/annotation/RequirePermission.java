package com.smartrecruit.common.annotation;

import java.lang.annotation.*;

/**
 * 权限验证注解。
 *
 * <p>标记在 Controller 方法上，用于声明该方法需要的权限码。
 * 配合 AOP 切面或 Spring Security 的 {@code @PreAuthorize} 使用，
 * 实现细粒度的 RBAC 权限控制。
 *
 * <p>使用示例：
 * <pre>{@code
 * @RequirePermission("user:create")
 * @PostMapping
 * public ApiResponse<UserVO> createUser(@RequestBody CreateUserRequest request) { ... }
 * }</pre>
 *
 * @author xdh
 * @since 1.0.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限码，格式为 {@code module:action}。
     *
     * <p>例如：{@code "user:create"}、{@code "job:delete"}、{@code "offer:approve"}。
     */
    String value();

    /**
     * 权限描述，用于日志和文档说明。
     */
    String description() default "";
}
