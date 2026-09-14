package com.smartrecruit.common.exception;

import java.io.Serial;

/**
 * 已认证用户缺少足够权限时抛出。
 *
 * <p>默认 HTTP 状态码：403 Forbidden</p>
 *
 * @since 1.0.0
 */
public non-sealed class ForbiddenException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "FORBIDDEN";

    /** 缺失的具体权限。 */
    private final String requiredPermission;

    public ForbiddenException(String message) {
        super(DEFAULT_ERROR_CODE, message);
        this.requiredPermission = null;
    }

    public ForbiddenException(String message, String requiredPermission) {
        super(DEFAULT_ERROR_CODE,
                requiredPermission != null
                        ? message + "（所需权限: " + requiredPermission + "）"
                        : message);
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }

    /**
     * 创建缺少特定权限的异常。
     */
    public static ForbiddenException missingPermission(String permission) {
        return new ForbiddenException(
                "访问被拒绝：缺少权限 '" + permission + "'", permission);
    }
}
