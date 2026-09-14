package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Offer接受度预测结果。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResult {

    /**
     * 候选人 ID。
     */
    private Long candidateId;
    /**
     * Offer ID。
     */
    private Long offerId;
    /**
     * 接受 Offer 的概率（0-100）。
     */
    private Double acceptanceProbability;
    /**
     * 各影响因素及其权重映射。
     */
    private Map<String, Double> factorInfluences;
    /**
     * 风险级别：LOW、MEDIUM、HIGH。
     */
    private String riskLevel;
    /**
     * 建议行动方案。
     */
    private String recommendation;
    /**
     * 提高接受率的具体建议列表。
     */
    private Map<String, Object> suggestions;
}
