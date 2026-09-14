package com.smartrecruit.offer.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.offer.dto.remote.UserDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * 系统服务 HTTP 接口客户端，用于发送邮件等内部调用。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface SystemClient {

    /** 通用邮件发送接口，参数: {to, subject, content, html}。 */
    @PostExchange("/internal/email/send")
    ApiResponse<Void> sendEmail(@RequestBody com.smartrecruit.offer.dto.remote.EmailRequest request);

    /** 查询用户信息（用于获取审批人和 Offer 创建人的邮箱）。 */
    @GetExchange("/users/{id}")
    ApiResponse<UserDTO> getUser(@PathVariable("id") Long id);

    /** 创建系统用户（入职账号开通时调用）。 */
    @PostExchange("/users")
    ApiResponse<Map<String, Object>> createUser(@RequestBody com.smartrecruit.offer.dto.remote.CreateUserRequest request);

    /** 发送站内通知（业务事件通知，如 Offer 审批）。 */
    @PostExchange("/internal/notifications")
    ApiResponse<Void> sendNotification(@RequestBody com.smartrecruit.offer.dto.remote.NotificationRequest request);

    /** 查询所有启用状态的管理员用户 ID（超级管理员 + HR 管理员）。 */
    @GetExchange("/internal/users/admins")
    ApiResponse<List<Long>> getAdminIds();

    /** 根据用户名查询用户 ID。 */
    @GetExchange("/internal/users/by-username")
    ApiResponse<Long> getUserIdByUsername(@RequestParam String username);
}
