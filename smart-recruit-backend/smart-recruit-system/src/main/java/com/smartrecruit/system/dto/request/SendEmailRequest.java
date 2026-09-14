package com.smartrecruit.system.dto.request;

import lombok.Data;

/**
 * 发送邮件请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class SendEmailRequest {

    /** 收件人邮箱。 */
    private String to;

    /** 邮件主题。 */
    private String subject;

    /** 邮件正文。 */
    private String content;

    /** 是否为 HTML 邮件（缺省 true）。 */
    private Boolean html;
}
