package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 求职申请实体，映射 {@code rec_application} 表。
 *
 * <p>将候选人与职位相关联，跟踪申请在招聘
 * 各阶段的生命周期。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_application", autoResultMap = true)
public class Application implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 {@code rec_candidate.id}。 */
    private Long candidateId;

    /** 关联 {@code rec_job_position.id}。 */
    @TableField("job_position_id")
    private Long jobId;

    /** 当前招聘阶段，如 RESUME_SCREEN、INTERVIEWING。 */
    @TableField("current_stage")
    private Integer stage;

    /** AI 生成的匹配分数（0-100）。 */
    @TableField("match_score")
    private Integer aiMatchScore;

    /** 候选人投递时间。 */
    @TableField("apply_time")
    private LocalDateTime applyAt;

    /** 阶段最后更新时间。非物理列。 */
    @TableField(exist = false)
    private LocalDateTime stageUpdatedAt;

    /** 内部备注。非物理列。 */
    @TableField(exist = false)
    private String notes;

    /** 记录创建时间。 */
    @TableField("create_time")
    private LocalDateTime createdAt;

    /** 逻辑删除标记：0=正常，1=已删除。 */
    @TableLogic
    private Integer deleted = 0;

}
