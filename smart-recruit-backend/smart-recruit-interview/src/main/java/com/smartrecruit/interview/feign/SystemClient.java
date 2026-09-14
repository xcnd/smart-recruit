package com.smartrecruit.interview.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.remote.SendEmailRequest;
import com.smartrecruit.interview.dto.remote.SendNotificationRequest;
import com.smartrecruit.interview.dto.remote.UserDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * 系统服务 HTTP 接口客户端，用于获取用户信息、发送邮件和系统通知。
 *
 * <p>核心邮件接口为 {@link #sendEmail(Map)}，调用系统模块的通用邮件发送端点。
 * 其他业务方法（如 sendAssessmentEmail）为便捷封装。</p>
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface SystemClient {

    @GetExchange("/users/{id}")
    ApiResponse<UserDTO> getUser(@PathVariable("id") Long id);

    /** 查询启用状态的管理员用户 ID 列表（超级管理员 + HR 管理员）。 */
    @GetExchange("/internal/users/admins")
    ApiResponse<List<Long>> getAdminUserIds();

    /** 通用邮件发送接口，参数: {to, subject, content, html}。 */
    @PostExchange("/internal/email/send")
    ApiResponse<Void> sendEmail(@RequestBody com.smartrecruit.interview.dto.remote.EmailMessageRequest request);

    /** 发送在线测评邀请邮件（便捷方法）。 */
    @PostExchange("/internal/email/send-assessment")
    ApiResponse<Void> sendAssessmentEmail(@RequestBody com.smartrecruit.interview.dto.remote.SendEmailRequest request);

    @PostExchange("/internal/notifications")
    ApiResponse<Void> sendNotification(@RequestBody SendNotificationRequest request);
}
