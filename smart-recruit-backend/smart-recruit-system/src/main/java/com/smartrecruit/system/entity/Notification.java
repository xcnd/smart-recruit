package com.smartrecruit.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知实体，映射到 {@code sys_notification} 表。
 *
 * <p>类型：INFO（信息）、WARNING（警告）、SUCCESS（成功）、ERROR（错误）。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@TableName("sys_notification")
public class Notification implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收者用户 ID。映射到 {@code recipient_id} 列。 */
    @TableField("recipient_id")
    private Long userId;

    /** 通知标题。 */
    private String title;

    /** 通知正文内容。 */
    private String content;

    /**
     * 通知类型。
     * 可选值：INFO、WARNING、SUCCESS、ERROR
     */
    private Integer type;

    /**
     * 已读状态：0 = 未读，1 = 已读。映射到 {@code is_read} 列。
     *
     * <p>属性命名使用 {@code isRead} 而非 {@code read}，因为 MySQL 中
     * {@code READ} 是保留字，MyBatis-Plus 生成的 {@code is_read AS read}
     * 别名会导致 SQL 语法错误。</p>
     */
    @TableField("is_read")
    private Integer isRead;

    /** 阅读时间。 */
    @TableField("read_time")
    private LocalDateTime readTime;

    /** 点击通知后的跳转地址（前端路由，如 /offers/123）。 */
    @TableField("action_url")
    private String actionUrl;

    /** 关联业务类型，如 OFFER_APPROVAL_REQUEST。 */
    @TableField("business_type")
    private String businessType;

    /** 关联业务 ID，如 Offer ID。 */
    @TableField("business_id")
    private Long businessId;

    /** 记录创建时间。映射到 {@code create_time} 列。 */
    @TableField("create_time")
    private LocalDateTime createdAt;
}
