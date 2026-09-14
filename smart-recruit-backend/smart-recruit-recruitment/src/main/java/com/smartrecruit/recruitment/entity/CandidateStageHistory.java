package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 候选人阶段历史实体，映射 {@code rec_candidate_stage_history} 表。
 *
 * <p>候选人阶段变更的审计日志（如 筛选 -&gt; 面试、
 * 面试 -&gt; 发放 offer），包含执行变更的操作人信息。</p>
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "rec_candidate_stage_history", autoResultMap = true)
public class CandidateStageHistory implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联 {@code rec_candidate.id}。 */
    private Long candidateId;

    /** 关联 {@code rec_application.id}。 */
    private Long applicationId;

    /** 变更前的招聘阶段。 */
    private Integer fromStage;

    /** 变更后的招聘阶段。 */
    private Integer toStage;

    /** 变更前的状态。 */
    private Integer fromStatus;

    /** 变更后的状态。 */
    private Integer toStatus;

    /** 执行阶段变更的用户 ID。 */
    private Long operatorId;

    /** 执行阶段变更的用户名称。 */
    private String operatorName;

    /** 阶段变更的可选备注或原因。 */
    private String remark;

    /** 记录创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
