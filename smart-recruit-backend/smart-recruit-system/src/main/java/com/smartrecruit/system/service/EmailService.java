package com.smartrecruit.system.service;

/**
 * 邮件发送服务。
 *
 * @since 1.0.0
 */
public interface EmailService {

    /** 发送普通邮件。 */
    void sendEmail(String to, String subject, String content, boolean html);

    /** 发送邮箱验证码邮件。 */
    void sendVerificationCode(String to, String code);

    /** 发送密码重置验证码邮件。 */
    void sendPasswordResetCode(String to, String code);

    /** 发送在线测评邀请邮件。 */
    void sendAssessmentEmail(String to, String candidateName, String jobTitle,
                             String typeLabel, String assessmentUrl);
}
