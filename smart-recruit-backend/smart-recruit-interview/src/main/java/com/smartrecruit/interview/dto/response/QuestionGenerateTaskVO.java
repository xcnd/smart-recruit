package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步出题任务状态 VO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionGenerateTaskVO {

    /** 任务 ID。 */
    private String taskId;

    /** 任务状态：PENDING, PROCESSING, COMPLETED, FAILED。 */
    private String status;

    /** 出题结果（仅 COMPLETED 时有值）。 */
    private QuestionGenerateResult result;

    /** 错误信息（仅 FAILED 时有值）。 */
    private String errorMessage;
}
