package com.smartrecruit.recruitment.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建工作台待办任务请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
public class CreateWorkbenchTaskRequest {

    /** 任务归属用户 ID（缺省 0，表示系统）。 */
    private Long userId;

    /** 任务标题。 */
    private String title;

    /** 任务描述。 */
    private String description;

    /** 任务类型：0=筛选简历,1=安排面试,2=审批Offer,3=办理入职,4=反馈评审,5=其他。 */
    private Integer type;

    /** 优先级：0=高,1=中,2=低。 */
    private Integer priority;

    /** 关联业务类型：CANDIDATE、INTERVIEW、OFFER、ONBOARDING。 */
    private String relatedType;

    /** 关联业务 ID。 */
    private Long relatedId;

    /** 关联候选人姓名。 */
    private String candidateName;

    /** 截止日期（yyyy-MM-dd HH:mm:ss）。 */
    private LocalDateTime dueDate;
}
