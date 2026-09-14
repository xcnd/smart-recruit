package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Size;

/**
 * 更新角色请求 DTO。
 *
 * @since 2026-04-26
 */
public record UpdateRoleRequest(
        @Size(max = 32, message = "角色名称最多32个字符")
        String name,

        @Size(max = 256, message = "描述最多256个字符")
        String description,

        Integer sortOrder
) {
}
