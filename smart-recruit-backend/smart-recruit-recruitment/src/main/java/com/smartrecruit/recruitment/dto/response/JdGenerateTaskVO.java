package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 生成 JD 异步任务视图对象。
 *
 * @since 2026-04-10
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdGenerateTaskVO {

    /** 任务 ID。 */
    private String taskId;

    /** 任务状态：PENDING / PROCESSING / COMPLETED / FAILED。 */
    private String status;

    /** 失败原因（FAILED 时返回）。 */
    private String message;

    /** 生成的职位标题。 */
    private String title;

    /** 生成的职位描述（COMPLETED 后返回）。 */
    private String description;
}
