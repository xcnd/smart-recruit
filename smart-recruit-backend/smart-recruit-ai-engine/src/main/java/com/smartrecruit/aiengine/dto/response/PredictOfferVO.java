package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Map;

/**
 * AI Offer 接受度预测视图对象。
 *
 * @param acceptanceProbability 接受 Offer 的概率（0-100）
 * @param riskLevel             风险级别：LOW、MEDIUM、HIGH
 * @param recommendation        建议行动方案
 * @param factorInfluences      影响因素及权重映射
 * @since 2026-04-07
 */
@JsonPropertyOrder({"acceptanceProbability", "riskLevel", "recommendation", "factorInfluences"})
public record PredictOfferVO(
        @JsonProperty("acceptanceProbability") Double acceptanceProbability,
        @JsonProperty("riskLevel") String riskLevel,
        @JsonProperty("recommendation") String recommendation,
        @JsonProperty("factorInfluences") Map<String, Double> factorInfluences
) {
}
