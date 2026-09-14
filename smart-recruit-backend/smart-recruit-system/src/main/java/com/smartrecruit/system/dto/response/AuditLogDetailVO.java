package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 审计日志详情 VO（包含请求参数、User-Agent、链路 ID 等完整信息）。
 *
 * @since 2026-04-08
 */
public record AuditLogDetailVO(
        @JsonProperty("id") Long id,
        @JsonProperty("userId") Long userId,
        @JsonProperty("username") String username,
        @JsonProperty("module") Integer module,
        @JsonProperty("moduleLabel") String moduleLabel,
        @JsonProperty("action") Integer action,
        @JsonProperty("actionLabel") String actionLabel,
        @JsonProperty("targetType") String targetType,
        @JsonProperty("targetId") Long targetId,
        @JsonProperty("description") String description,
        @JsonProperty("requestMethod") String requestMethod,
        @JsonProperty("requestUri") String requestUri,
        @JsonProperty("requestParams") String requestParams,
        @JsonProperty("responseStatus") Integer responseStatus,
        @JsonProperty("clientIp") String clientIp,
        @JsonProperty("userAgent") String userAgent,
        @JsonProperty("durationMs") Long durationMs,
        @JsonProperty("errorMsg") String errorMsg,
        @JsonProperty("traceId") String traceId,
        @JsonProperty("createTime") String createTime
) {
}
