package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 更新部门请求 DTO。
 *
 * @since 2026-04-26
 */
public record UpdateDeptRequest(
        @Size(max = 64, message = "部门名称最多64个字符")
        String name,

        Long parentId,

        Long leaderId,

        Integer sortOrder,

        @Size(max = 256, message = "备注最多256个字符")
        String remark
) {
}
