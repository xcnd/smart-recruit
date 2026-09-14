package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建部门请求 DTO。
 *
 * @since 2026-04-26
 */
public record CreateDeptRequest(
        @NotBlank(message = "部门名称不能为空")
        @Size(max = 64, message = "部门名称最多64个字符")
        String name,

        @NotBlank(message = "部门编码不能为空")
        @Size(max = 32, message = "部门编码最多32个字符")
        String code,

        Long parentId,

        Long leaderId,

        Integer sortOrder,

        @Size(max = 256, message = "备注最多256个字符")
        String remark
) {
}
