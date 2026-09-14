package com.smartrecruit.system.service;

/**
 * 邮箱验证码服务（生成、发送与校验）。
 *
 * @since 1.0.0
 */
public interface VerificationCodeService {

    /** 生成并发送默认用途的验证码。 */
    void generateAndSend(String email);

    /** 生成并发送指定用途的验证码。 */
    void generateAndSend(String email, String purpose);

    /** 校验默认用途的验证码。 */
    void verify(String email, String code);

    /** 校验指定用途的验证码。 */
    void verify(String email, String code, String purpose);
}
