package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 候选人画像与职位之间的内推匹配结果。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralMatch {

    /**
     * 候选人 ID。
     */
    private Long candidateId;
    /**
     * 匹配到的职位 ID。
     */
    private Long matchedJobId;
    /**
     * 匹配到的职位名称。
     */
    private String matchedJobTitle;
    /**
     * 匹配度评分（0-100）。
     */
    private Double matchScore;
    /**
     * 各维度匹配度映射。
     */
    private Map<String, Double> matchDimensions;
    /**
     * 推荐建议。
     */
    private String recommendation;
    /**
     * 建议的内推策略。
     */
    private String suggestedApproach;
}
