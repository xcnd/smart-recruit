package com.smartrecruit.system.service.impl;

import com.smartrecruit.common.constant.ConfigKeys;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import com.smartrecruit.system.service.EmailService;
import com.smartrecruit.system.service.SysConfigService;

/**
 * 邮件发送服务。
 *
 * <p>提供统一的邮件发送能力，核心方法为 {@link #sendEmail(String, String, String, boolean)}。
 * 其他业务方法（验证码、密码重置、测评邀请等）均基于此核心方法构建。
 * 对外通过 {@code POST /api/v1/internal/email/send} 暴露通用发送接口。</p>
 *
 * @author xdh
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String defaultFrom;

    @Value("${smart-recruit.system-name:SmartRecruit}")
    private String defaultSystemName;

    private final SysConfigService sysConfigService;

    // ================================================================
    // 核心通用接口 — 供内部服务调用
    // ================================================================

    /**
     * 发送邮件（统一的底层方法）。
     *
     * @param to      收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     * @param html    内容是否为 HTML 格式
     */
    public void sendEmail(String to, String subject, String content, boolean html) {
        String fromAddress = resolveFrom();
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("================================================================");
            log.warn("SMTP 未配置 (spring.mail.username 为空)，邮件内容输出到控制台：");
            log.warn("收件人: {}", to);
            log.warn("主题: {}", subject);
            log.warn("格式: {}", html ? "HTML" : "纯文本");
            log.warn("--------------------------------------------------------------");
            log.warn("内容 (已截断): {}", content.substring(0, Math.min(content.length(), 500)));
            log.warn("================================================================");
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, resolveSenderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, html);
            mailSender.send(message);
            log.info("邮件发送成功: to={}, subject={}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("邮件发送失败: to={}, subject={}", to, subject, e);
            throw new RuntimeException("邮件发送失败，请稍后重试", e);
        }
    }

    /**
     * 发送邮箱验证码（注册场景）。
     *
     * @param to   收件人邮箱
     * @param code 6 位验证码
     */
    public void sendVerificationCode(String to, String code) {
        String name = resolveSystemName();
        sendEmail(to, "【" + name + "】邮箱验证码", buildVerificationCodeContent(to, code, name), true);
    }

    /**
     * 发送密码重置验证码。
     *
     * @param to   收件人邮箱
     * @param code 6 位验证码
     */
    public void sendPasswordResetCode(String to, String code) {
        String name = resolveSystemName();
        sendEmail(to, "【" + name + "】密码重置验证码", buildPasswordResetContent(to, code, name), true);
    }

    /**
     * 构建专业的 HTML 邮件内容。
     */
    private String buildVerificationCodeContent(String to, String code, String systemName) {
        // 邮箱地址脱敏显示
        String maskedEmail = maskEmail(to);

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <!-- 主容器 -->
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                                    <!-- 头部 -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1890ff,#722ed1);padding:32px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">
                                                %s
                                            </h1>
                                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                                                智能招聘管理平台
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 内容区 -->
                                    <tr>
                                        <td style="padding:32px 40px;">
                                            <p style="margin:0 0 8px;color:#1a1a2e;font-size:16px;font-weight:600;">
                                                邮箱验证
                                            </p>
                                            <p style="margin:0 0 24px;color:#666;font-size:14px;line-height:1.8;">
                                                您好，<strong>%s</strong><br>
                                                您正在注册 %s 账号，请使用以下验证码完成身份验证：
                                            </p>

                                            <!-- 验证码框 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#f0f5ff;border:2px dashed #1890ff;border-radius:8px;padding:20px;text-align:center;">
                                                        <span style="font-family:'SF Mono','Menlo','Consolas',monospace;font-size:36px;font-weight:700;color:#1890ff;letter-spacing:8px;">
                                                            %s
                                                        </span>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 提示信息 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:20px;">
                                                <tr>
                                                    <td style="padding:12px 16px;background-color:#fff7e6;border-left:3px solid #fa8c16;border-radius:4px;">
                                                        <p style="margin:0;color:#8d6e00;font-size:12px;line-height:1.6;">
                                                            &#x23F0; <strong>有效期：</strong>5 分钟内有效，请尽快完成验证<br>
                                                            <strong>安全提示：</strong>请勿将验证码透露给他人
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 说明文字 -->
                                            <p style="margin:24px 0 0;color:#999;font-size:12px;line-height:1.6;">
                                                如非本人操作，请忽略此邮件。验证码过期后自动失效，无需任何操作。
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 底部 -->
                                    <tr>
                                        <td style="background-color:#fafafa;padding:20px 40px;text-align:center;border-top:1px solid #eef0f6;">
                                            <p style="margin:0;color:#aaa;font-size:11px;line-height:1.8;">
                                                %s &mdash; AI-Driven Intelligent Recruitment Platform<br>
                                                &copy; 2026 SmartRecruit Team. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(systemName, maskedEmail, systemName, code, systemName);
    }

    /**
     * 构建密码重置 HTML 邮件内容。
     */
    private String buildPasswordResetContent(String to, String code, String systemName) {
        String maskedEmail = maskEmail(to);

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <!-- 主容器 -->
                                <table role="presentation" width="480" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                                    <!-- 头部 -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#1890ff,#722ed1);padding:32px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">
                                                %s
                                            </h1>
                                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                                                智能招聘管理平台
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 内容区 -->
                                    <tr>
                                        <td style="padding:32px 40px;">
                                            <p style="margin:0 0 8px;color:#1a1a2e;font-size:16px;font-weight:600;">
                                                密码重置
                                            </p>
                                            <p style="margin:0 0 24px;color:#666;font-size:14px;line-height:1.8;">
                                                您好，<strong>%s</strong><br>
                                                您正在申请重置 %s 账号的登录密码，请使用以下验证码完成身份验证：
                                            </p>

                                            <!-- 验证码框 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                <tr>
                                                    <td style="background-color:#f0f5ff;border:2px dashed #1890ff;border-radius:8px;padding:20px;text-align:center;">
                                                        <span style="font-family:'SF Mono','Menlo','Consolas',monospace;font-size:36px;font-weight:700;color:#1890ff;letter-spacing:8px;">
                                                            %s
                                                        </span>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 提示信息 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-top:20px;">
                                                <tr>
                                                    <td style="padding:12px 16px;background-color:#fff7e6;border-left:3px solid #fa8c16;border-radius:4px;">
                                                        <p style="margin:0;color:#8d6e00;font-size:12px;line-height:1.6;">
                                                            &#x23F0; <strong>有效期：</strong>5 分钟内有效，请尽快完成重置<br>
                                                            <strong>安全提示：</strong>请勿将验证码透露给他人<br>
                                                            &#x26A0; <strong>重要提醒：</strong>如非本人操作，您的账号可能存在安全风险
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 说明文字 -->
                                            <p style="margin:24px 0 0;color:#999;font-size:12px;line-height:1.6;">
                                                如非本人操作，请忽略此邮件并及时登录检查账号安全。验证码过期后自动失效，无需任何操作。
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 底部 -->
                                    <tr>
                                        <td style="background-color:#fafafa;padding:20px 40px;text-align:center;border-top:1px solid #eef0f6;">
                                            <p style="margin:0;color:#aaa;font-size:11px;line-height:1.8;">
                                                %s &mdash; AI-Driven Intelligent Recruitment Platform<br>
                                                &copy; 2026 SmartRecruit Team. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(systemName, maskedEmail, systemName, code, systemName);
    }

    /**
     * 发送在线测评邀请邮件。
     *
     * @param to            候选人邮箱
     * @param candidateName 候选人姓名
     * @param jobTitle      应聘职位
     * @param typeLabel     测评类型（如"编程测试"）
     * @param assessmentUrl 测评链接（含 token）
     */
    public void sendAssessmentEmail(String to, String candidateName, String jobTitle,
                                     String typeLabel, String assessmentUrl) {
        String name = resolveSystemName();
        String subject = "【" + name + "】在线测评邀请 — " + jobTitle;
        sendEmail(to, subject,
                buildAssessmentInviteContent(to, candidateName, jobTitle, typeLabel,
                        assessmentUrl, name), true);
    }

    /**
     * 构建在线测评邀请 HTML 邮件内容。
     */
    private String buildAssessmentInviteContent(String to, String candidateName,
                                                 String jobTitle, String typeLabel,
                                                 String assessmentUrl, String systemName) {
        String maskedEmail = maskEmail(to);
        String typeIcon = "";

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <meta name="color-scheme" content="light">
                    <meta name="supported-color-schemes" content="light">
                </head>
                <body style="margin:0;padding:0;background-color:#f5f7fa;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,'Helvetica Neue',Arial,'PingFang SC','Microsoft YaHei',sans-serif;">
                    <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f5f7fa;padding:40px 0;">
                        <tr>
                            <td align="center">
                                <!-- 主容器 -->
                                <table role="presentation" width="520" cellpadding="0" cellspacing="0" style="background-color:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
                                    <!-- 头部 -->
                                    <tr>
                                        <td style="background:linear-gradient(135deg,#4f46e5,#7c3aed);padding:36px 40px;text-align:center;">
                                            <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:700;letter-spacing:1px;">
                                                %s
                                            </h1>
                                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:13px;">
                                                AI-Driven Intelligent Recruitment Platform
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 内容区 -->
                                    <tr>
                                        <td style="padding:36px 40px;">
                                            <!-- 问候 -->
                                            <p style="margin:0 0 4px;color:#1a1a2e;font-size:16px;font-weight:600;">
                                                %s 您好，
                                            </p>
                                            <p style="margin:0 0 24px;color:#475569;font-size:14px;line-height:1.8;">
                                                感谢您对 <strong>%s</strong> 职位的关注。为更全面地评估您的能力匹配度，诚邀您完成以下在线测评：
                                            </p>

                                            <!-- 测评信息卡片 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;margin-bottom:24px;">
                                                <tr>
                                                    <td style="padding:16px 20px;">
                                                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0">
                                                            <tr>
                                                                <td style="padding:6px 0;font-size:13px;color:#64748b;width:80px;">应聘职位</td>
                                                                <td style="padding:6px 0;font-size:14px;color:#0f172a;font-weight:600;">%s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:6px 0;font-size:13px;color:#64748b;">测评类型</td>
                                                                <td style="padding:6px 0;font-size:14px;color:#0f172a;">%s %s</td>
                                                            </tr>
                                                            <tr>
                                                                <td style="padding:6px 0;font-size:13px;color:#64748b;">发送邮箱</td>
                                                                <td style="padding:6px 0;font-size:13px;color:#0f172a;">%s</td>
                                                            </tr>
                                                        </table>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- CTA 按钮 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:24px;">
                                                <tr>
                                                    <td align="center">
                                                        <a href="%s" target="_blank" rel="noopener noreferrer" style="display:inline-block;padding:14px 48px;background:linear-gradient(135deg,#4f46e5,#6366f1);color:#ffffff;font-size:15px;font-weight:700;text-decoration:none;border-radius:8px;letter-spacing:0.5px;box-shadow:0 4px 12px rgba(79,70,229,0.35);">
                                                            &#x25B6; 开始在线测评
                                                        </a>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 备选链接 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:20px;">
                                                <tr>
                                                    <td style="padding:12px 16px;background-color:#f0f5ff;border:1px solid #e0e7ff;border-radius:6px;">
                                                        <p style="margin:0 0 6px;color:#475569;font-size:12px;font-weight:600;">
                                                            如按钮无法点击，请复制以下链接到浏览器打开：
                                                        </p>
                                                        <p style="margin:0;color:#4f46e5;font-size:12px;word-break:break-all;line-height:1.6;">
                                                            %s
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 重要提示 -->
                                            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="margin-bottom:4px;">
                                                <tr>
                                                    <td style="padding:14px 16px;background-color:#fffbeb;border-left:3px solid #f59e0b;border-radius:4px;">
                                                        <p style="margin:0;color:#92400e;font-size:12px;line-height:1.8;">
                                                            &#x23F0; <strong>有效期：</strong>本链接 7 天内有效，请及时完成测评<br>
                                                            <strong>设备建议：</strong>编程类测评建议使用电脑端完成<br>
                                                            <strong>安全提示：</strong>本链接与您的个人信息绑定，请勿转发他人
                                                        </p>
                                                    </td>
                                                </tr>
                                            </table>

                                            <!-- 说明文字 -->
                                            <p style="margin:24px 0 0;color:#94a3b8;font-size:12px;line-height:1.6;">
                                                此为系统自动发送的邮件，请勿直接回复。如有任何疑问，请联系招聘负责人。
                                            </p>
                                        </td>
                                    </tr>
                                    <!-- 底部 -->
                                    <tr>
                                        <td style="background-color:#fafafa;padding:20px 40px;text-align:center;border-top:1px solid #f1f5f9;">
                                            <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.8;">
                                                %s &mdash; AI-Driven Intelligent Recruitment Platform<br>
                                                &copy; 2026 SmartRecruit Team. All rights reserved.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                systemName,                          // 头部标题
                candidateName,                       // 候选人姓名
                jobTitle,                            // 职位名
                jobTitle,                            // 测评卡片-职位
                typeIcon, typeLabel,                 // 测评卡片-类型
                maskedEmail,                         // 测评卡片-邮箱
                assessmentUrl,                       // CTA 按钮链接
                assessmentUrl,                       // 备选链接
                systemName                           // 底部
        );
    }

    /**
     * 邮箱地址脱敏。
     *
     * <p>示例：admin@smartrecruit.com → a***n@smartrecruit.com</p>
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    /**
     * 解析系统名称：优先取系统配置，缺失时回退到本地配置。
     */
    private String resolveSystemName() {
        String configured = sysConfigService.getString(ConfigKeys.SYSTEM_NAME, "");
        return configured.isBlank() ? defaultSystemName : configured;
    }

    /**
     * 解析发件人地址。
     *
     * <p>绝大多数 SMTP 服务商（163、QQ、企业邮等）要求 MAIL FROM 必须等于
     * 登录账号，否则返回 {@code 553 Mail from must equal authorized user}。
     * 因此发件地址固定使用 {@code spring.mail.username}（SMTP 登录账号），
     * 展示名称仍可通过系统配置 {@code email_sender_name} 自定义。</p>
     */
    private String resolveFrom() {
        return defaultFrom;
    }

    /**
     * 解析发件人显示名称：优先取系统配置。
     */
    private String resolveSenderName() {
        String configured = sysConfigService.getString(ConfigKeys.EMAIL_SENDER_NAME, "");
        return configured.isBlank() ? resolveSystemName() : configured;
    }
}
