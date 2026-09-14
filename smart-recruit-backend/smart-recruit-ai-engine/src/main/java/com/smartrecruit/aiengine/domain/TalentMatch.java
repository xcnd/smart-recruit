package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 人才库匹配结果（一个候选人针对目标职位的匹配结论）。
 *
 * @since 2026-04-09
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TalentMatch {

    /** 候选人 ID。 */
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** 匹配评分（0-100）。 */
    private Double matchScore;
    /** 推荐建议。 */
    private String recommendation;
    /** 各维度匹配度映射。 */
    private Map<String, Double> matchDimensions;
}
