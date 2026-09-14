package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 权限视图 VO。
 *
 * @since 2026-04-26
 */
@JsonPropertyOrder({"id", "name", "code", "type", "module", "parentId", "path", "component",
        "icon", "method", "sortOrder", "status", "visible", "createTime"})
public record PermissionVO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("name")
        String name,

        @JsonProperty("code")
        String code,

        @JsonProperty("type")
        Integer type,

        @JsonProperty("module")
        Integer module,

        @JsonProperty("parentId")
        Long parentId,

        @JsonProperty("path")
        String path,

        @JsonProperty("component")
        String component,

        @JsonProperty("icon")
        String icon,

        @JsonProperty("method")
        Integer method,

        @JsonProperty("sortOrder")
        Integer sortOrder,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("visible")
        Integer visible,

        @JsonProperty("createTime")
        LocalDateTime createTime
) {
}
