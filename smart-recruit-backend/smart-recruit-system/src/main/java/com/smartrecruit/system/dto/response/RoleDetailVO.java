package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色详情 VO。
 *
 * @since 2026-04-26
 */
public record RoleDetailVO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("name")
        String name,

        @JsonProperty("code")
        String code,

        @JsonProperty("description")
        String description,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("sortOrder")
        Integer sortOrder,

        @JsonProperty("permissionIds")
        List<Long> permissionIds,

        @JsonProperty("createTime")
        LocalDateTime createTime,

        @JsonProperty("updateTime")
        LocalDateTime updateTime
) {
}
