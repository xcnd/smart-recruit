package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 发送系统通知请求 DTO — 通过 Feign 调用 system 模块的通知接口。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    /** 接收人用户 ID。 */
    private Long userId;
    /** 批量接收人用户 ID 列表（与 userId 二选一）。 */
    private List<Long> userIds;
    /** 通知标题。 */
    private String title;
    /** 通知内容。 */
    private String content;
    /** 通知类型：1=面试,2=Offer,3=入职,4=内推,5=人才。 */
    private String type;
    /** 业务子类型，如 INTERVIEW_SCHEDULED。 */
    private String businessType;
    /** 关联业务 ID。 */
    private Long businessId;
    /** 点击跳转地址（前端路由）。 */
    private String actionUrl;
}
