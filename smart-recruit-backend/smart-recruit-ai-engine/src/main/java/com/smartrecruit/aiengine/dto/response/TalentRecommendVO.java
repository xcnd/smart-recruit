package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * AI 人才推荐结果 VO（一个候选人 → 目标职位的匹配结论）。
 *
 * @param candidateId     候选人 ID
 * @param candidateName   候选人姓名
 * @param matchScore      匹配评分（0-100）
 * @param recommendation  推荐建议
 * @param matchDimensions 各维度匹配度映射
 * @since 2026-04-09
 */
public record TalentRecommendVO(
        @JsonProperty("candidateId") Long candidateId,
        @JsonProperty("candidateName") String candidateName,
        @JsonProperty("matchScore") Double matchScore,
        @JsonProperty("recommendation") String recommendation,
        @JsonProperty("matchDimensions") Map<String, Double> matchDimensions) {
}
