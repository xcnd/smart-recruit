package com.smartrecruit.common.exception;

import java.io.Serial;

/**
 * 尝试创建已存在的资源时抛出。
 *
 * <p>默认 HTTP 状态码：409 Conflict</p>
 *
 * @since 1.0.0
 */
public non-sealed class DuplicateResourceException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "DUPLICATE_RESOURCE";

    public DuplicateResourceException(String resourceName, String duplicateField, String value) {
        super(DEFAULT_ERROR_CODE,
                "资源 '" + resourceName + "' 已存在，" + duplicateField + ": " + value);
    }

    public DuplicateResourceException(String message) {
        super(DEFAULT_ERROR_CODE, message);
    }

    public DuplicateResourceException(String errorCode, String message) {
        super(errorCode, message);
    }
}
