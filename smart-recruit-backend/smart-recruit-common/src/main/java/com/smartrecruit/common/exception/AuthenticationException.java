package com.smartrecruit.common.exception;

import java.io.Serial;

/**
 * 认证失败时抛出（凭证无效、Token 过期等）。
 *
 * <p>默认 HTTP 状态码：401 Unauthorized</p>
 *
 * @since 1.0.0
 */
public non-sealed class AuthenticationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "AUTHENTICATION_FAILED";

    public AuthenticationException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public AuthenticationException(String errorCode, String message) {
        super(errorCode, message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(DEFAULT_ERROR_CODE, message, cause);
    }

    /**
     * 创建 Token 过期场景的异常。
     */
    public static AuthenticationException tokenExpired() {
        return new AuthenticationException("TOKEN_EXPIRED", "认证令牌已过期，请重新登录");
    }

    /**
     * 创建凭证无效场景的异常。
     */
    public static AuthenticationException invalidCredentials() {
        return new AuthenticationException("INVALID_CREDENTIALS", "用户名或密码错误");
    }

    /**
     * 创建缺少 Token 场景的异常。
     */
    public static AuthenticationException tokenMissing() {
        return new AuthenticationException("TOKEN_MISSING", "缺少认证令牌");
    }
}
