package com.smartrecruit.common.dto;

import com.smartrecruit.common.util.DateUtils;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * 标准 API 响应包装类，用于所有 REST 接口。
 *
 * <p>平台中的所有控制器都返回此类型，确保前端客户端获得一致的响应结构。
 * 所有业务异常均通过此类的 error 工厂方法封装，HTTP 状态码统一返回 200。</p>
 *
 * <pre>{@code
 * // 成功响应
 * ApiResponse<UserVO> ok = ApiResponse.success(userVO);
 * // {"success":true,"code":200,"message":"ok","data":{...},"timestamp":"..."}
 *
 * // 业务异常响应
 * ApiResponse<Void> err = ApiResponse.error(40001, "用户名或密码错误");
 * // {"success":false,"code":40001,"message":"用户名或密码错误","timestamp":"..."}
 *
 * // 从 BusinessException 构建
 * ApiResponse<Void> err = ApiResponse.from(ex);
 * // {"success":false,"code":401,"message":"Invalid username or password","timestamp":"..."}
 * }</pre>
 *
 * @param <T> 负载数据类型
 * @since 1.0.0
 */
@JsonPropertyOrder({"success", "code", "message", "data", "timestamp"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        @JsonProperty("success")
        boolean ok,

        @JsonProperty("code")
        int code,

        @JsonProperty("message")
        String message,

        @JsonProperty("data")
        T data,

        @JsonProperty("timestamp")
        Instant timestamp
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // ================================================================
    // 常量
    // ================================================================

    public static final int CODE_SUCCESS = 0;
    private static final String MESSAGE_SUCCESS = "ok";

    // ================================================================
    // 工厂方法 — 成功
    // ================================================================

    /** 创建成功响应（带数据）。 */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, CODE_SUCCESS, MESSAGE_SUCCESS, data, DateUtils.currentInstant());
    }

    /** 创建成功响应（自定义消息）。 */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, CODE_SUCCESS, message, data, DateUtils.currentInstant());
    }

    /** 创建成功响应（无数据）。 */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, CODE_SUCCESS, MESSAGE_SUCCESS, null, DateUtils.currentInstant());
    }

    // ================================================================
    // 工厂方法 — 错误
    // ================================================================

    /** 从 BusinessException 构建错误响应。 */
    public static <T> ApiResponse<T> from(com.smartrecruit.common.exception.BusinessException ex) {
        int errorCode = BusinessErrorCodeMapper.toCode(ex.getErrorCode());
        return new ApiResponse<>(false, errorCode, ex.getMessage(), null, DateUtils.currentInstant());
    }

    /** 创建错误响应。 */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(false, code, message, null, DateUtils.currentInstant());
    }

    /** 创建带数据的错误响应。 */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(false, code, message, data, DateUtils.currentInstant());
    }

    // ================================================================
    // 业务错误码映射
    // ================================================================

    /**
     * 将字符串错误码映射为整数业务错误码。
     *
     * <p>映射规则：</p>
     * <ul>
     *   <li>40001~40099 — 认证相关</li>
     *   <li>40101~40199 — 权限相关</li>
     *   <li>40201~40299 — 资源相关</li>
     *   <li>40301~40399 — 验证相关</li>
     *   <li>40901~40999 — 重复/冲突</li>
     *   <li>50001 — 未知内部错误</li>
     * </ul>
     */
    static final class BusinessErrorCodeMapper {
        private BusinessErrorCodeMapper() {}

        static int toCode(String errorCode) {
            if (errorCode == null) return 50001;
            return switch (errorCode) {
                case "INVALID_CREDENTIALS"                -> 40001;
                case "AUTHENTICATION_FAILED"              -> 40002;
                case "TOKEN_EXPIRED"                      -> 40003;
                case "TOKEN_MISSING"                      -> 40004;
                case "FORBIDDEN"                          -> 40101;
                case "ACCESS_DENIED"                      -> 40102;
                case "RESOURCE_NOT_FOUND"                 -> 40201;
                case "VALIDATION_FAILED"                  -> 40301;
                case "CONSTRAINT_VIOLATION"               -> 40302;
                case "METHOD_VALIDATION_FAILED"           -> 40303;
                case "DUPLICATE_RESOURCE"                 -> 40901;
                case "USER_NOT_FOUND"                     -> 40202;
                case "RATE_LIMITED"                       -> 40029;
                case "GATEWAY_TIMEOUT"                    -> 50002;
                case "BAD_GATEWAY"                        -> 50003;
                default                                   -> 50001;
            };
        }
    }
}
