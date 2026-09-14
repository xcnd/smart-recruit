package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 手机号验证码登录请求 DTO。
 * 适用于非员工（候选人）用户的快捷登录，首次登录自动注册。
 *
 * @since 2026-04-01
 */
public record PhoneLoginRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        @NotBlank(message = "验证码不能为空")
        String verificationCode
) {
}
