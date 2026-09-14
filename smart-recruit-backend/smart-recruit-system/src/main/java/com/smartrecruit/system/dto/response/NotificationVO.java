package com.smartrecruit.system.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统通知展示 VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知 ID。 */
    private Long id;
    /** 接收用户 ID。 */
    private Long userId;
    /** 通知标题。 */
    private String title;
    /** 通知内容。 */
    private String content;
    /** 通知类型：INFO、WARNING、SUCCESS、ERROR。 */
    private Integer type;
    /**
     * 是否已读：0 = 未读，1 = 已读。
     * 内部属性为 {@code isRead}，JSON 序列化仍输出 {@code read}，兼容前端。
     */
    @JsonProperty("read")
    private Integer isRead;
    /** 阅读时间。 */
    private LocalDateTime readTime;
    /** 点击跳转地址（前端路由）。 */
    private String actionUrl;
    /** 关联业务类型。 */
    private String businessType;
    /** 关联业务 ID。 */
    private Long businessId;
    /** 创建时间。 */
    private LocalDateTime createdAt;
}
