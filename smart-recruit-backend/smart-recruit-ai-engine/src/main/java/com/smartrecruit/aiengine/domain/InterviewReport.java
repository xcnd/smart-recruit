package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI生成的面试评估报告。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewReport {

    /**
     * 面试会话 ID。
     */
    private Long sessionId;
    /**
     * 综合评分（0-100）。
     */
    private Double overallScore;
    /**
     * 各维度评分映射（维度名 → 评分）。
     */
    private Map<String, Double> dimensionScores;
    /**
     * 沟通能力评语。
     */
    private String communicationAssessment;
    /**
     * 技术能力评语。
     */
    private String technicalAssessment;
    /**
     * 行为面试评语。
     */
    private String behavioralAssessment;
    /**
     * 候选人优势列表。
     */
    private String strengths;
    /**
     * 候选人不足列表。
     */
    private String weaknesses;
    /**
     * 录用建议：HIRE、CONSIDER、REJECT。
     */
    private String hireRecommendation;
    /**
     * AI 评估置信度（0-100）。
     */
    private Double confidenceLevel;
}
