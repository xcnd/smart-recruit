package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 创建工作台待办任务请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchTaskRequest {

    /** 任务归属用户 ID。 */
    private Long userId;

    /** 任务标题。 */
    private String title;

    /** 任务描述。 */
    private String description;

    /** 任务类型编码。 */
    private Integer type;

    /** 优先级编码。 */
    private Integer priority;

    /** 关联业务类型。 */
    private String relatedType;

    /** 关联业务 ID。 */
    private Long relatedId;

    /** 关联候选人姓名。 */
    private String candidateName;

    /** 截止日期。 */
    private LocalDateTime dueDate;
}
