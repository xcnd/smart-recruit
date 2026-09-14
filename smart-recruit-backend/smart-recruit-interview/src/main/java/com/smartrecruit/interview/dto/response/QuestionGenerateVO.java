package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * AI 异步出题任务提交结果视图对象。
 *
 * @param taskId 异步生成任务 ID，前端据此轮询任务状态
 * @since 2026-04-07
 */
@JsonPropertyOrder({"taskId"})
public record QuestionGenerateVO(
        @JsonProperty("taskId") String taskId
) {
}
