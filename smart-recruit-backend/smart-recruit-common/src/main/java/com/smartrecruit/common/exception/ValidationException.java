package com.smartrecruit.common.exception;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 输入校验失败时抛出。
 *
 * <p>默认 HTTP 状态码：400 Bad Request</p>
 *
 * @since 1.0.0
 */
public non-sealed class ValidationException extends BusinessException {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "VALIDATION_FAILED";

    /** 字段级校验错误。 */
    private final transient Map<String, List<String>> fieldErrors;

    public ValidationException(String message) {
        super(DEFAULT_ERROR_CODE, message);
        this.fieldErrors = Map.of();
    }

    public ValidationException(String message, Map<String, List<String>> fieldErrors) {
        super(DEFAULT_ERROR_CODE, message, fieldErrors);
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public ValidationException(String fieldName, String errorDetail) {
        super(DEFAULT_ERROR_CODE,
                "字段 '" + fieldName + "' 验证失败: " + errorDetail);
        this.fieldErrors = Map.of(fieldName, List.of(errorDetail));
    }

    public Map<String, List<String>> getFieldErrors() {
        return fieldErrors;
    }
}
