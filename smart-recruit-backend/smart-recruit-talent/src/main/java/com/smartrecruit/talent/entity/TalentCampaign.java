package com.smartrecruit.talent.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 人才活动实体，映射 {@code rec_talent_campaign} 表。
 *
 * <p>管理针对人才库候选人的推广活动，通过邮件、短信或其他渠道触达，并跟踪触达与响应指标。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_talent_campaign")
public class TalentCampaign implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 活动显示名称。 */
    private String campaignName;

    /** 消息模板类型：JOB_ALERT、NEWSLETTER、EVENT_INVITE、SEASONAL。 */
    private Integer templateType;

    /** 目标人群描述或分段筛选条件。 */
    private String targetAudience;

    /** 目标接收人数。 */
    private Integer targetCount;

    /** 活动消息内容（可包含占位符）。 */
    private String messageContent;

    /** 发送方式：EMAIL、SMS、PUSH。 */
    private Integer sendMethod;

    /** 活动计划执行时间。 */
    private LocalDateTime scheduledTime;

    /** 活动实际发送时间。 */
    private LocalDateTime sentTime;

    /** 成功触达的接收人数。 */
    private Integer reachCount;

    /** 已响应的接收人数。 */
    private Integer responseCount;

    /** 响应率（已响应 / 已触达）。 */
    private BigDecimal responseRate;

    /** 状态：DRAFT、SCHEDULED、SENDING、SENT、COMPLETED、CANCELLED。 */
    private Integer status;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    private Long createUserId;

    /** 创建人 ID。 */
    private String createBy;

    /** 更新人 ID。 */
    private Long updateUserId;

    /** 更新人 ID。 */
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
