package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * AI 生成的面试题视图对象。
 *
 * @param question     题目文本
 * @param type         面试类型编码
 * @param round        面试轮次
 * @param jobTitle     职位名称
 * @param scoreStandard 评分标准
 * @since 2026-04-07
 */
@JsonPropertyOrder({"question", "type", "round", "jobTitle", "scoreStandard"})
public record InterviewQuestionVO(
        @JsonProperty("question") String question,
        @JsonProperty("type") Integer type,
        @JsonProperty("round") Integer round,
        @JsonProperty("jobTitle") String jobTitle,
        @JsonProperty("scoreStandard") String scoreStandard
) {
}
