package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO。
 *
 * @author xdh
 * @since 1.0.0
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 2, max = 32, message = "用户名长度应为2-32个字符")
        @Pattern(regexp = "^[a-zA-Z0-9_\\-\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线、中划线及中文")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度应为6-64个字符")
        String password,

        @NotBlank(message = "真实姓名不能为空")
        @Size(min = 1, max = 32, message = "姓名长度应为1-32个字符")
        String realName,

        @NotBlank(message = "验证码不能为空")
        String verificationCode,

        @Size(max = 8, message = "内推码长度应不超过8个字符")
        String referralCode
) {
}
