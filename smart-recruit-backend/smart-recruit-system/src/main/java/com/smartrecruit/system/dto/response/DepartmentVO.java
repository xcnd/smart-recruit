package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;

/**
 * 部门视图 VO。
 *
 * @since 2026-04-26
 */
@JsonPropertyOrder({"id", "name", "code", "parentId", "leaderId", "sortOrder", "status", "remark", "createTime"})
public record DepartmentVO(
        @JsonProperty("id")
        Long id,

        @JsonProperty("name")
        String name,

        @JsonProperty("code")
        String code,

        @JsonProperty("parentId")
        Long parentId,

        @JsonProperty("leaderId")
        Long leaderId,

        @JsonProperty("sortOrder")
        Integer sortOrder,

        @JsonProperty("status")
        Integer status,

        @JsonProperty("remark")
        String remark,

        @JsonProperty("createTime")
        LocalDateTime createTime
) {
}
