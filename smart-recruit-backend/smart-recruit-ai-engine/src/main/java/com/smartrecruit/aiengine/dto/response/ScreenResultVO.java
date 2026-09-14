package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * AI 智能筛选结果视图对象。
 *
 * @param overallScore    综合匹配评分（0-100）
 * @param passed          是否通过筛选
 * @param recommendation  筛选建议
 * @param summary         筛选结论摘要
 * @param dimensionScores 各维度评分映射（维度名 → 分值）
 * @since 2026-04-07
 */
@JsonPropertyOrder({"overallScore", "passed", "recommendation", "summary", "dimensionScores"})
public record ScreenResultVO(
        @JsonProperty("overallScore") Double overallScore,
        @JsonProperty("passed") Boolean passed,
        @JsonProperty("recommendation") String recommendation,
        @JsonProperty("summary") String summary,
        @JsonProperty("dimensionScores") Map<String, Double> dimensionScores
) {
}
