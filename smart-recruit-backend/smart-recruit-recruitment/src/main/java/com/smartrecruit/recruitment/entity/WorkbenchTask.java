package com.smartrecruit.recruitment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工作台待办任务实体，映射 {@code rec_workbench_task} 表。
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("rec_workbench_task")
public class WorkbenchTask implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 任务归属人ID */
    private Long userId;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 任务类型：0=筛选简历,1=安排面试,2=审批Offer,3=办理入职,4=反馈评审,5=其他 */
    private Integer type;

    /** 优先级：0=高,1=中,2=低 */
    private Integer priority;

    /** 关联业务类型：CANDIDATE,INTERVIEW,OFFER,ONBOARDING */
    private String relatedType;

    /** 关联业务ID */
    private Long relatedId;

    /** 关联候选人姓名 */
    private String candidateName;

    /** 截止日期 */
    private LocalDateTime dueDate;

    /** 状态：0=待办,1=已完成,2=已忽略 */
    private Integer status;

    /** 完成时间 */
    private LocalDateTime completedAt;

    /** 创建时间。 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
