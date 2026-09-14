package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求 DTO。
 *
 * @since 2026-05-09
 */
public record ChangePasswordRequest(
        @NotBlank(message = "当前密码不能为空")
        String oldPassword,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为6-64个字符")
        String newPassword
) {
}
