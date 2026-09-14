package com.smartrecruit.system.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartrecruit.system.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * {@code sys_notification} 表的 Mapper 接口。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {

    /**
     * 列出某个用户的所有通知，按创建时间倒序排列。
     */
    List<Notification> selectByUserId(@Param("userId") Long userId);

    /**
     * 统计某个用户的未读通知数量。
     */
    int countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 将某个用户的所有通知标记为已读。
     */
    int updateReadAllByUserId(@Param("userId") Long userId);
}
