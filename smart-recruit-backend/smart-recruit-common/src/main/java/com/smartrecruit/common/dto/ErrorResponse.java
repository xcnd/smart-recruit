package com.smartrecruit.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 结构化的错误详情 DTO，在发生错误时通过 {@link ApiResponse} 返回。
 *
 * @param errorCode   机器可读的错误代码，用于国际化/客户端路由
 * @param message     人类可读的错误描述
 * @param path        触发错误的请求 URI
 * @param timestamp   错误发生的时间
 * @param fieldErrors 可选的字段级验证错误信息
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        @JsonProperty("errorCode")
        String errorCode,

        @JsonProperty("message")
        String message,

        @JsonProperty("path")
        String path,

        @JsonProperty("timestamp")
        LocalDateTime timestamp,

        @JsonProperty("fieldErrors")
        Map<String, List<String>> fieldErrors
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 不含字段级错误信息的紧凑构造函数。
     */
    public ErrorResponse(String errorCode, String message, String path, LocalDateTime timestamp) {
        this(errorCode, message, path, timestamp, Collections.emptyMap());
    }

    /**
     * 返回字段错误信息的不可变副本。
     */
    @Override
    public Map<String, List<String>> fieldErrors() {
        return fieldErrors != null ? Collections.unmodifiableMap(fieldErrors) : Collections.emptyMap();
    }
}
