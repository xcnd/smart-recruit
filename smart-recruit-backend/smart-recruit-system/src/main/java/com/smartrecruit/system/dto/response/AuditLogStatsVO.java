package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 审计日志统计 VO。
 *
 * @param total        日志总条数
 * @param todayCount   今日新增条数
 * @param successCount 成功条数
 * @param failCount    失败条数
 * @param successRate  成功率（%）
 * @param avgDurationMs 平均耗时（毫秒）
 * @since 2026-04-08
 */
public record AuditLogStatsVO(
        @JsonProperty("total") long total,
        @JsonProperty("todayCount") long todayCount,
        @JsonProperty("successCount") long successCount,
        @JsonProperty("failCount") long failCount,
        @JsonProperty("successRate") double successRate,
        @JsonProperty("avgDurationMs") double avgDurationMs
) {
}
