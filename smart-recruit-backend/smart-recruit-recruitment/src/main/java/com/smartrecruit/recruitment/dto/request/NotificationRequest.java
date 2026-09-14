package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

/**
 * 发送站内通知请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class NotificationRequest {

    /** 接收人用户 ID。 */
    private Long userId;

    /** 通知标题。 */
    private String title;

    /** 通知内容。 */
    private String content;

    /** 通知类型编码。 */
    private Integer type;

    /** 业务类型（如 INTERVIEW、OFFER、ONBOARDING）。 */
    private String businessType;

    /** 关联业务 ID。 */
    private Long businessId;

    /** 点击跳转地址。 */
    private String actionUrl;
}
