package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 招聘渠道实体，映射 {@code rec_job_channel} 表。
 *
 * <p>表示职位的发布渠道，跟踪
 * 发布/取消发布的生命周期和状态。</p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rec_job_channel", autoResultMap = true)
public class JobChannel implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 {@code rec_job_position.id}。 */
    private Long jobPositionId;

    /** 外部渠道标识。 */
    private Long channelId;

    /** 可读的渠道名称，如 "Boss 直聘"。 */
    private String channelName;

    /** 渠道类型：INTERNAL、EXTERNAL、SOCIAL、CAMPUS。 */
    private Integer channelType;

    /** 职位在该渠道的发布 URL。 */
    private String publishUrl;

    /** 职位发布到该渠道的时间。 */
    private LocalDateTime publishTime;

    /** 职位从该渠道取消发布的时间。 */
    private LocalDateTime unpublishTime;

    /** 状态：0=未激活，1=已激活，2=已过期。 */
    private Integer status;

    /** 记录创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 最后更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
