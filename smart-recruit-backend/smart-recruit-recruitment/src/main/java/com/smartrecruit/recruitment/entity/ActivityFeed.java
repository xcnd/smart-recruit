package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动动态实体，映射 {@code rec_activity_feed} 表。
 *
 * <p>表示一条按时间排列的动态条目，描述用户对相关业务对象
 *（职位、候选人、申请等）执行的操作。</p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rec_activity_feed", autoResultMap = true)
public class ActivityFeed implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 活动类型：CREATED、UPDATED、COMMENTED、STATUS_CHANGED 等。 */
    private Integer type;

    /** 活动简短标题。 */
    private String title;

    /** 活动详细描述。 */
    private String description;

    /** 执行操作的用户 ID。 */
    private Long actorId;

    /** 执行操作的用户名称。 */
    private String actorName;

    /** 关联业务对象类型：JOB、CANDIDATE、APPLICATION 等。 */
    private String relatedType;

    /** 关联业务对象 ID。 */
    private Long relatedId;

    /** 记录创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
