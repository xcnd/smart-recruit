package com.smartrecruit.recruitment.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 待办任务视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class PendingTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /** 任务归属人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /** 任务标题 */
    private String title;

    /** 任务描述 */
    private String description;

    /** 任务类型：0=筛选简历,1=安排面试,2=审批Offer,3=办理入职,4=反馈评审,5=其他 */
    private Integer type;

    /** 优先级：0=高,1=中,2=低 */
    private Integer priority;

    /** 关联业务类型 */
    private String relatedType;

    /** 关联业务ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long relatedId;

    /** 关联候选人姓名 */
    private String candidateName;

    /** 截止日期 */
    private LocalDateTime dueDate;

    /** 状态：0=待办,1=已完成,2=已忽略 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
