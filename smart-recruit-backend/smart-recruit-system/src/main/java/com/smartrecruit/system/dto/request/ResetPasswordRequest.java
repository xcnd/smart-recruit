package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 密码重置请求 DTO。
 *
 * @author xdh
 * @since 1.0.0
 */
public record ResetPasswordRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "验证码不能为空")
        String verificationCode,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为6-64个字符")
        String newPassword
) {
}
