package com.smartrecruit.system.dto.request;

import lombok.Data;

import java.util.List;

/**
 * 发送站内通知请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class SendInternalNotificationRequest {

    /** 通知标题。 */
    private String title;

    /** 通知内容。 */
    private String content;

    /** 通知类型编码。 */
    private Integer type;

    /** 业务类型（如 INTERVIEW、OFFER、ONBOARDING）。 */
    private String businessType;

    /** 点击跳转地址。 */
    private String actionUrl;

    /** 关联业务 ID。 */
    private Long businessId;

    /** 单个接收人用户 ID（与 userIds 二选一）。 */
    private Long userId;

    /** 批量接收人用户 ID 列表（与 userId 二选一）。 */
    private List<Long> userIds;
}
