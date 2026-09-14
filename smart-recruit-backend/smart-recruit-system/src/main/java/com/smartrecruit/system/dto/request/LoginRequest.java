package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO。
 *
 * @since 2026-04-26
 */
public record LoginRequest(
        @NotBlank(message = "账号不能为空")
        String account,

        @NotBlank(message = "密码不能为空")
        String password,

        String captchaId,

        String captchaCode
) {
}
