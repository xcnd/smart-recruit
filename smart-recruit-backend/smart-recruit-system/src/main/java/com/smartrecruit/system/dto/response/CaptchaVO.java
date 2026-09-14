package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 图片验证码响应 VO。
 *
 * @since 2026-05-09
 */
public record CaptchaVO(
        @JsonProperty("captchaId") String captchaId,
        @JsonProperty("captchaImage") String captchaImage
) {
}
