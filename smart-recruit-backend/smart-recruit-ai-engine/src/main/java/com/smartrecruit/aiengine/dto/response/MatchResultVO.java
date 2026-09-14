package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * AI 内推职位匹配结果视图对象。
 *
 * @param matchedJobId     匹配到的职位 ID
 * @param matchedJobTitle  匹配到的职位名称
 * @param matchScore       匹配评分（0-100）
 * @param recommendation   推荐建议
 * @param suggestedApproach 建议的内推策略
 * @param matchDimensions  各维度匹配度映射
 * @since 2026-04-07
 */
@JsonPropertyOrder({"matchedJobId", "matchedJobTitle", "matchScore", "recommendation",
        "suggestedApproach", "matchDimensions"})
public record MatchResultVO(
        @JsonProperty("matchedJobId") Long matchedJobId,
        @JsonProperty("matchedJobTitle") String matchedJobTitle,
        @JsonProperty("matchScore") Double matchScore,
        @JsonProperty("recommendation") String recommendation,
        @JsonProperty("suggestedApproach") String suggestedApproach,
        @JsonProperty("matchDimensions") Map<String, Double> matchDimensions
) {
}
