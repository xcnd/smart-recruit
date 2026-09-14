package com.smartrecruit.system.controller;

import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ForbiddenException;
import com.smartrecruit.common.util.UserContextUtil;
import com.smartrecruit.system.dto.request.BroadcastNotificationRequest;
import com.smartrecruit.system.dto.request.SendInternalNotificationRequest;
import com.smartrecruit.system.dto.response.NotificationVO;
import com.smartrecruit.system.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 系统通知 REST 控制器。
 *
 * <p>对外提供当前用户的分页通知列表、未读计数、单条/全部已读、
 * 最近通知预览与管理员公告广播；内部端点供其他微服务在业务事件
 * 发生时投递站内通知（INNER-REQUEST 放行）。</p>
 *
 * @since 2026-04-26
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    // ==================== 对外接口（需鉴权） ====================

    /**
     * 分页查询当前用户的通知。
     *
     * <p>查看的是本人通知，任何已登录用户均可访问，不要求额外权限。</p>
     */
    @GetMapping("/api/v1/notifications")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PageResult<NotificationVO>> listNotifications(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer read) {
        Long currentUserId = UserContextUtil.getCurrentUserIdOrThrow();
        if (userId != null && !userId.equals(currentUserId)) {
            throw new ForbiddenException("只能查看自己的通知");
        }
        PageResult<NotificationVO> result =
                notificationService.pageQuery(currentUserId, page, size, type, read);
        return ApiResponse.success(result);
    }

    /**
     * 查询当前用户最近 N 条通知（顶部下拉预览）。
     */
    @GetMapping("/api/v1/notifications/recent")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<NotificationVO>> recent(
            @RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(notificationService.listRecent(UserContextUtil.getCurrentUserIdOrThrow(), limit));
    }

    /**
     * 查询当前用户未读通知数。
     */
    @GetMapping("/api/v1/notifications/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Long> unreadCount() {
        return ApiResponse.success(notificationService.unreadCount(UserContextUtil.getCurrentUserIdOrThrow()));
    }

    /**
     * 将一条通知标记为已读。
     */
    @PutMapping("/api/v1/notifications/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(UserContextUtil.getCurrentUserIdOrThrow(), id);
        return ApiResponse.success();
    }

    /**
     * 将当前用户所有未读通知标记为已读。
     */
    @PutMapping("/api/v1/notifications/read-all")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(UserContextUtil.getCurrentUserIdOrThrow());
        return ApiResponse.success();
    }

    /**
     * 管理员广播系统公告（发送给全部启用用户）。
     */
    @PostMapping("/api/v1/notifications/broadcast")
    @PreAuthorize("hasAuthority('notification:edit')")
    public ApiResponse<Void> broadcast(@RequestBody BroadcastNotificationRequest request) {
        String title = request.getTitle() != null ? request.getTitle() : "";
        String content = request.getContent() != null ? request.getContent() : "";
        Integer type = request.getType() != null
                ? request.getType() : NotificationConstants.TYPE_SYSTEM;
        String actionUrl = request.getActionUrl();
        if (title.isBlank()) {
            return ApiResponse.error(400, "公告标题不能为空");
        }
        notificationService.broadcast(title, content, type,
                NotificationConstants.BIZ_ANNOUNCEMENT, actionUrl);
        return ApiResponse.success("公告已发布", null);
    }

    // ==================== 内部接口（服务间调用，INNER-REQUEST 放行） ====================

    /**
     * 内部服务调用 — 创建一条（或批量）系统通知。
     */
    @PostMapping("/api/v1/internal/notifications")
    public ApiResponse<Void> sendInternalNotification(@RequestBody SendInternalNotificationRequest request) {
        String title = request.getTitle() != null ? request.getTitle() : "";
        String content = request.getContent() != null ? request.getContent() : "";
        Integer type = request.getType() != null
                ? request.getType() : NotificationConstants.TYPE_SYSTEM;
        String businessType = request.getBusinessType();
        String actionUrl = request.getActionUrl();
        Long businessId = request.getBusinessId();

        List<Long> userIds = new ArrayList<>();
        if (request.getUserId() != null) {
            userIds.add(request.getUserId());
        }
        if (request.getUserIds() != null) {
            userIds.addAll(request.getUserIds());
        }
        if (userIds.isEmpty()) {
            return ApiResponse.error(400, "userId 或 userIds 不能为空");
        }
        if (title.isBlank()) {
            return ApiResponse.error(400, "通知标题不能为空");
        }

        notificationService.sendBatch(userIds, title, content, type,
                businessType, businessId, actionUrl);
        return ApiResponse.success("通知已发送", null);
    }

}
