package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送验证码请求 DTO。
 *
 * <p>{@code purpose} 字段区分验证码用途（注册 / 密码重置），
 * 默认值为 {@code "register"} 以保持向前兼容。</p>
 *
 * @author xdh
 * @since 1.0.0
 */
public record SendCodeRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        String purpose
) {
    /**
     * 紧凑构造器：purpose 为空时默认为注册场景。
     */
    public SendCodeRequest {
        if (purpose == null || purpose.isBlank()) {
            purpose = "register";
        }
    }
}
