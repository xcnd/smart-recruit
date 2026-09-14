package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送普通邮件请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageRequest {

    /** 收件人邮箱。 */
    private String to;

    /** 邮件主题。 */
    private String subject;

    /** 邮件正文。 */
    private String content;

    /** 是否为 HTML 邮件。 */
    private Boolean html;
}
