package com.smartrecruit.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.constant.NotificationConstants;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.common.exception.ForbiddenException;
import com.smartrecruit.common.exception.ResourceNotFoundException;
import com.smartrecruit.common.util.StringUtils;
import com.smartrecruit.system.dto.response.NotificationVO;
import com.smartrecruit.system.entity.Notification;
import com.smartrecruit.system.entity.SysUser;
import com.smartrecruit.system.repository.NotificationMapper;
import com.smartrecruit.system.repository.SysUserMapper;
import com.smartrecruit.system.service.NotificationService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 系统通知服务实现。
 *
 * @since 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final SysUserMapper sysUserMapper;

    /** 发送通知。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, String title, String content, String type) {
        send(userId, title, content, parseType(type), null, null, null);
    }

    /** 发送通知。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, String title, String content, Integer type) {
        send(userId, title, content, type, null, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Long userId, String title, String content, Integer type,
                     String businessType, Long businessId, String actionUrl) {
        if (userId == null) {
            log.warn("通知接收人为空，跳过发送: title={}", title);
            return;
        }
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("通知标题不能为空");
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content == null ? "" : content);
        notification.setType(resolveType(type));
        notification.setIsRead(0);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setActionUrl(actionUrl);
        notification.setCreatedAt(DateUtils.now());
        notificationMapper.insert(notification);
        log.info("通知已发送: id={}, userId={}, type={}, bizType={}",
                notification.getId(), userId, notification.getType(), businessType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendBatch(Collection<Long> userIds, String title, String content, Integer type,
                          String businessType, Long businessId, String actionUrl) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.stream().distinct().forEach(userId -> send(
                userId, title, content, type, businessType, businessId, actionUrl));
    }

    @Override
    public PageResult<NotificationVO> pageQuery(Long userId, int page, int size,
                                                Integer type, Integer read) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(type != null, Notification::getType, type)
                .eq(read != null, Notification::getIsRead, read)
                .orderByDesc(Notification::getCreatedAt);

        IPage<Notification> result = notificationMapper.selectPage(
                new Page<>(safePage, safeSize), wrapper);
        List<NotificationVO> records = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        return new PageResult<>(records, result.getTotal(), result.getSize(),
                result.getCurrent(), result.getPages());
    }

    /** 查询当前用户未读消息数量。 */
    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .eq(Notification::getIsRead, 0));
    }

    /** 标记消息为已读。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException("通知不存在: id=" + notificationId);
        }
        if (!notification.getUserId().equals(userId)) {
            throw new ForbiddenException("无权操作他人通知");
        }
        if (notification.getIsRead() != null && notification.getIsRead() == 1) {
            return;
        }
        notification.setIsRead(1);
        notification.setReadTime(DateUtils.now());
        notificationMapper.updateById(notification);
        log.info("通知已标记为已读: id={}, userId={}", notificationId, userId);
    }

    /** 将所有消息标记为已读。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        int updated = notificationMapper.updateReadAllByUserId(userId);
        log.info("全部通知已标记为已读: userId={}, updated={}", userId, updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void broadcast(String title, String content, Integer type,
                          String businessType, String actionUrl) {
        List<Long> userIds = sysUserMapper.selectList(
                        new LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, com.smartrecruit.common.constant.Constants.USER_STATUS_ACTIVE))
                .stream()
                .map(SysUser::getId)
                .toList();
        log.info("系统公告广播: 接收人数={}, title={}", userIds.size(), title);
        sendBatch(userIds, title, content, type, businessType, null, actionUrl);
    }

    /** 查询最近通知列表。 */
    @Override
    public List<NotificationVO> listRecent(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 50);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt)
                .last("LIMIT " + safeLimit);
        return notificationMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    private Integer parseType(String type) {
        if (type == null || type.isBlank()) {
            return NotificationConstants.TYPE_SYSTEM;
        }
        try {
            return Integer.parseInt(type.trim());
        } catch (NumberFormatException e) {
            log.warn("通知类型解析失败，使用系统类型: type={}", type);
            return NotificationConstants.TYPE_SYSTEM;
        }
    }

    private Integer resolveType(Integer type) {
        if (type == null) {
            return NotificationConstants.TYPE_SYSTEM;
        }
        return switch (type) {
            case NotificationConstants.TYPE_INTERVIEW,
                 NotificationConstants.TYPE_OFFER,
                 NotificationConstants.TYPE_ONBOARDING,
                 NotificationConstants.TYPE_REFERRAL,
                 NotificationConstants.TYPE_TALENT,
                 NotificationConstants.TYPE_RECRUITMENT -> type;
            default -> NotificationConstants.TYPE_SYSTEM;
        };
    }

    private NotificationVO toVO(Notification entity) {
        return NotificationVO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .isRead(entity.getIsRead())
                .readTime(entity.getReadTime())
                .actionUrl(entity.getActionUrl())
                .businessType(entity.getBusinessType())
                .businessId(entity.getBusinessId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
