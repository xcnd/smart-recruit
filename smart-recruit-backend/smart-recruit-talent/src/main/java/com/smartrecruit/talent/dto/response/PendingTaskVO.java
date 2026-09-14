package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 仪表盘待办任务项。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTaskVO {

    /** 任务ID。 */
    private Long id;

    /** 任务类别：面试、Offer审批、入职、文档。 */
    private Integer category;

    /** 任务标题。 */
    private String title;

    /** 任务优先级：高、中、低。 */
    private Integer priority;

    /** 涉及的候选人/员工姓名。 */
    private String relatedPerson;

    /** 截止日期。 */
    private LocalDateTime dueDate;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 关联业务类型：CANDIDATE, INTERVIEW, OFFER, ONBOARDING。 */
    private String relatedType;

    /** 关联业务ID。 */
    private Long relatedId;

    /** 任务描述。 */
    private String description;
}
