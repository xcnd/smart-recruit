package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建角色请求 DTO。
 *
 * @since 2026-04-26
 */
public record CreateRoleRequest(
        @NotBlank(message = "角色名称不能为空")
        @Size(max = 32, message = "角色名称最多32个字符")
        String name,

        @NotBlank(message = "角色编码不能为空")
        @Size(max = 32, message = "角色编码最多32个字符")
        String code,

        @Size(max = 256, message = "描述最多256个字符")
        String description,

        Integer sortOrder
) {
}
