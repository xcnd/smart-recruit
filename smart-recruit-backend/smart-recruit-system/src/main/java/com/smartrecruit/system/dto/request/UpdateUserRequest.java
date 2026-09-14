package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新用户请求 DTO。
 *
 * @since 2026-04-26
 */
public record UpdateUserRequest(
        @Size(max = 32, message = "姓名最多32个字符")
        String realName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 64, message = "邮箱最多64个字符")
        String email,

        String mobile,

        Long deptId,

        /** 单一角色 ID（兼容），与 roleIds 二选一。 */
        Long roleId,

        /** 角色 ID 列表（支持一用户多角色，非空时整体替换角色）。 */
        List<Long> roleIds,

        Integer gender,

        String position,

        String jobLevel,

        Integer age
) {
}
