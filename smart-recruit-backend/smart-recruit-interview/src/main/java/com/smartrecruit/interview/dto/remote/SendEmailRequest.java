package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送测评邮件请求 DTO — 通过 Feign 调用 system 模块的邮件接口。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {
    /** 收件人邮箱。 */
    private String to;
    /** 候选人姓名。 */
    private String candidateName;
    /** 应聘职位。 */
    private String jobTitle;
    /** 测评类型标签（如"编程测试"）。 */
    private String typeLabel;
    /** 测评链接（完整 URL，含 token）。 */
    private String assessmentUrl;
}
