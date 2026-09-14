package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 智能筛选过程的结果，包含五维评分。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScreeningResult {

    /**
     * 候选人 ID。
     */
    private Long candidateId;
    /**
     * 职位 ID。
     */
    private Long jobId;
    /**
     * 综合匹配评分（0-100）。
     */
    private Double overallScore;

    /**
     * 五维评分：技能匹配度、经验匹配度、学历匹配度、稳定性、综合匹配度
     */
    private Map<String, Double> dimensionScores;

    /**
     * 是否通过筛选。
     */
    private Boolean passed;
    /**
     * 筛选建议：STRONG_MATCH、MATCH、WEAK_MATCH、NO_MATCH。
     */
    private String recommendation;
    /**
     * 筛选结果摘要。
     */
    private String summary;
    /**
     * 简历亮点列表。
     */
    private Map<String, String> highlights;
}
