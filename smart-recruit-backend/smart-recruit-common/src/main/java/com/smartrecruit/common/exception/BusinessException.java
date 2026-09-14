package com.smartrecruit.common.exception;

/**
 * SmartRecruit 平台的业务异常基类，所有领域异常均应继承此类。
 *
 * <p>采用 Java 25 密封类层次结构，确保异常处理的完备性。</p>
 *
 * @since 1.0.0
 */
public sealed class BusinessException extends RuntimeException
        permits ResourceNotFoundException,
                ValidationException,
                AuthenticationException,
                ForbiddenException,
                DuplicateResourceException {

    /** 错误码，用于国际化与客户端处理。 */
    private final String errorCode;

    /** 可选的详情数据载荷。 */
    private final transient Object data;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }

    public BusinessException(String errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public BusinessException(String errorCode, String message, Object data, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = data;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
