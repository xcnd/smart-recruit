package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 修改邮箱请求 DTO。
 *
 * @since 2026-05-09
 */
public record ChangeEmailRequest(
        @NotBlank(message = "新邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String newEmail,

        @NotBlank(message = "验证码不能为空")
        String verificationCode
) {
}
