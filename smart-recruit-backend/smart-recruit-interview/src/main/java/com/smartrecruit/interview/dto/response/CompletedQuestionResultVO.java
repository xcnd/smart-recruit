package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * 已完成 AI 出题任务结果视图对象。
 *
 * @param taskId        异步生成任务 ID
 * @param positionLabel 岗位名称
 * @param positionType  岗位类型编码
 * @since 2026-04-07
 */
@JsonPropertyOrder({"taskId", "positionLabel", "positionType"})
public record CompletedQuestionResultVO(
        @JsonProperty("taskId") String taskId,
        @JsonProperty("positionLabel") String positionLabel,
        @JsonProperty("positionType") Integer positionType
) {
}
