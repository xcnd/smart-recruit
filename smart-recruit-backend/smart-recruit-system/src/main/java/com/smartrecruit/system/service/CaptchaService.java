package com.smartrecruit.system.service;

import com.smartrecruit.system.dto.response.CaptchaVO;

/**
 * 图形验证码服务。
 *
 * @since 1.0.0
 */
public interface CaptchaService {

    /** 生成图形验证码。 */
    CaptchaVO generateCaptcha();

    /** 校验验证码。 */
    boolean validate(String captchaId, String captchaCode);
}
