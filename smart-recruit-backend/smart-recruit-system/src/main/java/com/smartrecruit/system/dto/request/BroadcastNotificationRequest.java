package com.smartrecruit.system.dto.request;

import lombok.Data;

/**
 * 发布全员公告通知请求。
 *
 * @since 2026-04-07
 */
@Data
public class BroadcastNotificationRequest {

    /** 公告标题。 */
    private String title;

    /** 公告内容。 */
    private String content;

    /** 通知类型编码。 */
    private Integer type;

    /** 点击跳转地址。 */
    private String actionUrl;
}
