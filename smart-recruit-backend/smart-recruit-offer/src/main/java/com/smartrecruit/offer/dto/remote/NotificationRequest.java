package com.smartrecruit.offer.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送站内通知请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /** 接收人用户 ID。 */
    private Long userId;

    /** 批量接收人用户 ID（与 userId 二选一）。 */
    private List<Long> userIds;

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
