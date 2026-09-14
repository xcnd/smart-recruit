package com.smartrecruit.common.exception;

import java.io.Serial;

/**
 * 请求的资源不存在时抛出。
 *
 * <p>默认 HTTP 状态码：404 Not Found</p>
 *
 * @since 1.0.0
 */
public non-sealed class ResourceNotFoundException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(DEFAULT_ERROR_CODE,
                "资源 '" + resourceName + "' 不存在，标识: " + resourceId);
    }

    public ResourceNotFoundException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }
}
