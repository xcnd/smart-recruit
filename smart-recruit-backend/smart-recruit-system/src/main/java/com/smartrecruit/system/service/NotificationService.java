package com.smartrecruit.system.service;

import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.system.dto.response.NotificationVO;

import java.util.Collection;
import java.util.List;

/**
 * 系统通知服务接口。
 *
 * <p>提供站内通知的发送（单个/批量/全员广播）、分页查询、
 * 未读计数与已读管理能力；业务事件通知由各微服务通过
 * 内部接口调用本服务落库。</p>
 *
 * @since 2026-04-26
 */
public interface NotificationService {

    /**
     * 向单个用户发送通知（兼容旧签名，type 为字符串类型码）。
     */
    void send(Long userId, String title, String content, String type);

    /**
     * 向单个用户发送通知。
     *
     * @param userId  接收人用户 ID
     * @param title   通知标题
     * @param content 通知内容
     * @param type    业务分类（见 {@link com.smartrecruit.common.constant.NotificationConstants}）
     */
    void send(Long userId, String title, String content, Integer type);

    /**
     * 向单个用户发送完整通知（含业务关联与跳转地址）。
     */
    void send(Long userId, String title, String content, Integer type,
              String businessType, Long businessId, String actionUrl);

    /**
     * 向多个用户发送同一条通知。
     */
    void sendBatch(Collection<Long> userIds, String title, String content, Integer type,
                   String businessType, Long businessId, String actionUrl);

    /**
     * 分页查询用户通知。
     *
     * @param userId 接收人用户 ID
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param type   业务分类过滤（可选）
     * @param read   已读状态过滤：0=未读，1=已读（可选）
     */
    PageResult<NotificationVO> pageQuery(Long userId, int page, int size,
                                         Integer type, Integer read);

    /**
     * 查询用户未读通知数。
     */
    long unreadCount(Long userId);

    /**
     * 将指定通知标记为已读（校验通知归属）。
     *
     * @param userId         当前用户 ID
     * @param notificationId 通知 ID
     */
    void markRead(Long userId, Long notificationId);

    /**
     * 将用户所有未读通知标记为已读。
     */
    void markAllRead(Long userId);

    /**
     * 向全部启用用户广播系统公告（仅限管理员调用）。
     */
    void broadcast(String title, String content, Integer type,
                   String businessType, String actionUrl);

    /**
     * 查询某用户最近 N 条通知（顶部下拉预览用）。
     */
    List<NotificationVO> listRecent(Long userId, int limit);
}
