package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 创建权限请求 DTO。
 *
 * @since 2026-04-26
 */
public record CreatePermissionRequest(
        @NotBlank(message = "权限名称不能为空")
        @Size(max = 64, message = "权限名称最多64个字符")
        String name,

        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码最多128个字符")
        String code,

        @NotNull(message = "权限类型不能为空")
        Integer permType,

        @NotNull(message = "所属模块不能为空")
        Integer module,

        Long parentId,

        @Size(max = 256, message = "路径最多256个字符")
        String path,

        @Size(max = 256, message = "组件路径最多256个字符")
        String component,

        @Size(max = 64, message = "图标最多64个字符")
        String icon,

        Integer method,

        @Size(max = 256, message = "API路径最多256个字符")
        String apiPath,

        Integer sortOrder,

        Integer status,

        Integer visible
) {
}
