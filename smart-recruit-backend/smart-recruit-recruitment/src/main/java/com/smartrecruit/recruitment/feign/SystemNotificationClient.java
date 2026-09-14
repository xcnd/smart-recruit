package com.smartrecruit.recruitment.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * 系统服务通知 HTTP 接口客户端。
 *
 * <p>供招聘模块在简历上传、筛选结果等业务事件发生时
 * 向系统模块投递站内通知。</p>
 *
 * @since 2026-04-06
 */
@HttpExchange("/api/v1/internal/notifications")
public interface SystemNotificationClient {

    /**
     * 发送站内通知，参数：userId / userIds、title、content、type、
     * businessType、businessId、actionUrl。
     */
    @PostExchange
    ApiResponse<Void> send(@RequestBody com.smartrecruit.recruitment.dto.request.NotificationRequest request);
}
