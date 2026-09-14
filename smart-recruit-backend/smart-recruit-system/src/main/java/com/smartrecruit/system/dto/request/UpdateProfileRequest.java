package com.smartrecruit.system.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 个人信息更新请求 DTO（用户自助修改）。
 *
 * @since 2026-05-09
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 32, message = "姓名长度应为1-32个字符")
        String realName,

        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String mobile,

        Integer gender,

        String avatar
) {
}
