package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 角色视图 VO。
 *
 * @since 2026-04-26
 */
@JsonPropertyOrder({"id", "name", "code", "description", "status", "sortOrder", "createTime"})
public record RoleVO(
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

        @JsonProperty("createTime")
        LocalDateTime createTime
) {
}
