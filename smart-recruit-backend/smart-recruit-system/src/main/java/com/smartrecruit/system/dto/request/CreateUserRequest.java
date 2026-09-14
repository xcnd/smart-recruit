package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建用户请求 DTO。
 *
 * @since 2026-04-26
 */
public record CreateUserRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(max = 32, message = "用户名最多32个字符")
        String username,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 32, message = "姓名最多32个字符")
        String realName,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 64, message = "邮箱最多64个字符")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度6-32个字符")
        String password,

        @NotNull(message = "部门不能为空")
        Long deptId,

        String mobile,

        /** 单一角色 ID（兼容内部调用，如入职自动开通账号），与 roleIds 二选一。 */
        Long roleId,

        /** 角色 ID 列表（支持一用户多角色），优先于 roleId。 */
        List<Long> roleIds,

        Integer gender,

        String position,

        String jobLevel,

        Integer age
) {
}
