package com.smartrecruit.common.exception;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.ErrorResponse;
import com.smartrecruit.common.util.DateUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局异常处理器。
 *
 * <p>所有业务异常统一返回 HTTP 200，通过响应体中的 {@code success} 字段
 * 和 {@code code} 业务错误码区分成功与失败。</p>
 *
 * @since 1.0.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ================================================================
    // 业务异常 — 返回 HTTP 200 + 业务错误码
    // ================================================================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("业务异常 [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.ok(ApiResponse.from(ex));
    }

    // ================================================================
    // 验证异常
    // ================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.computeIfAbsent(fieldError.getField(), k -> new java.util.ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        log.debug("参数验证失败: {}", fieldErrors);

        var error = new ErrorResponse(
                "VALIDATION_FAILED",
                "请求体验证失败",
                request.getRequestURI(),
                DateUtils.now(),
                fieldErrors
        );

        return ResponseEntity.ok(ApiResponse.error(40301, "请求体验证失败", error));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        Map<String, List<String>> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String field = violation.getPropertyPath().toString();
            fieldErrors.computeIfAbsent(field, k -> new java.util.ArrayList<>())
                    .add(violation.getMessage());
        }

        log.debug("约束违反: {}", fieldErrors);

        var error = new ErrorResponse(
                "CONSTRAINT_VIOLATION",
                "参数验证失败",
                request.getRequestURI(),
                DateUtils.now(),
                fieldErrors
        );

        return ResponseEntity.ok(ApiResponse.error(40302, "参数验证失败", error));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest request) {

        log.debug("Handler 方法验证失败: {}", ex.getMessage());

        var error = new ErrorResponse(
                "METHOD_VALIDATION_FAILED",
                "方法参数验证失败: " + ex.getMessage(),
                request.getRequestURI(),
                DateUtils.now()
        );

        return ResponseEntity.ok(ApiResponse.error(40303, "方法参数验证失败", error));
    }

    // ================================================================
    // 安全异常
    // ================================================================

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex,
            HttpServletRequest request) {

        log.warn("访问被拒绝: {}", ex.getMessage());

        var error = new ErrorResponse(
                "ACCESS_DENIED",
                "您没有权限访问此资源",
                request.getRequestURI(),
                DateUtils.now()
        );

        return ResponseEntity.ok(ApiResponse.error(40102, "您没有权限访问此资源", error));
    }

    /**
     * 异步请求超时（SSE 流式连接自然断开/超时）。
     *
     * <p>SSE 连接超时或客户端断开属于正常现象，浏览器会自动重连；
     * 此处直接静默结束，不再向已声明 {@code text/event-stream} 的响应写 JSON，
     * 避免出现「No converter for ApiResponse」的二次报错。</p>
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        log.debug("异步请求超时（SSE 连接正常断开）: {}", ex.getMessage());
    }

    // ================================================================
    // 兜底异常
    // ================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("发生未预期的错误 [{}] {}", request.getMethod(), request.getRequestURI(), ex);

        var error = new ErrorResponse(
                "INTERNAL_ERROR",
                "发生了未预期的错误，请稍后重试。",
                request.getRequestURI(),
                DateUtils.now()
        );

        return ResponseEntity.ok(ApiResponse.error(50001, "发生了未预期的错误，请稍后重试。", error));
    }
}
