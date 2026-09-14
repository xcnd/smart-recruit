package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 审计日志列表条目 VO。
 *
 * @param id            日志主键
 * @param userId        操作人用户 ID
 * @param username      操作人用户名
 * @param module        操作模块编码
 * @param moduleLabel   操作模块名称（如「系统管理」）
 * @param action        操作动作编码
 * @param actionLabel   操作动作名称（如「创建」）
 * @param targetType    目标资源类型
 * @param targetId      目标资源 ID
 * @param description   操作描述
 * @param requestMethod HTTP 方法
 * @param requestUri    请求路径
 * @param responseStatus HTTP 响应状态码
 * @param clientIp      客户端 IP
 * @param durationMs    执行耗时（毫秒）
 * @param errorMsg      错误信息（成功时为 null）
 * @param createTime    操作时间（yyyy-MM-dd HH:mm:ss）
 * @since 2026-04-08
 */
public record AuditLogVO(
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
        @JsonProperty("responseStatus") Integer responseStatus,
        @JsonProperty("clientIp") String clientIp,
        @JsonProperty("durationMs") Long durationMs,
        @JsonProperty("errorMsg") String errorMsg,
        @JsonProperty("createTime") String createTime
) {
}
