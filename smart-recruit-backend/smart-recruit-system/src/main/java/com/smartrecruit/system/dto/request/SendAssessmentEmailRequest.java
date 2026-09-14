package com.smartrecruit.system.dto.request;

import lombok.Data;

/**
 * 发送在线测评邮件请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class SendAssessmentEmailRequest {

    /** 收件人邮箱。 */
    private String to;

    /** 候选人姓名。 */
    private String candidateName;

    /** 应聘职位。 */
    private String jobTitle;

    /** 测评类型标签。 */
    private String typeLabel;

    /** 测评访问链接。 */
    private String assessmentUrl;
}
