package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 部门树形视图 VO。
 *
 * @since 2026-04-26
 */
public record DepartmentTreeVO(
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

        @JsonProperty("children")
        List<DepartmentTreeVO> children
) {
}
