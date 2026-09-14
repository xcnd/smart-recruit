package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 角色统计 VO。
 *
 * @since 2026-05-10
 */
public record RoleStatsVO(
        @JsonProperty("total") long total,
        @JsonProperty("builtin") long builtin,
        @JsonProperty("custom") long custom
) {
}
