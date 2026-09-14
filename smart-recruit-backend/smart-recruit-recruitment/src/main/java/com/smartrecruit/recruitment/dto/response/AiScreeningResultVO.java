package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * AI驱动的简历筛选结果。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiScreeningResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 被筛选的简历ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long resumeId;

    /** 与简历关联的候选人ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** AI匹配综合分数（0-100）。 */
    private Integer overallScore;

    /** 各维度分数明细。 */
    private Map<String, Integer> dimensionScores;

    /** 简历中匹配到的关键词。 */
    private List<String> matchedKeywords;

    /** 简历中未匹配到的缺失关键词。 */
    private List<String> missingKeywords;

    /** 机器可读的推荐结果：STRONG_MATCH、MATCH、WEAK_MATCH、NO_MATCH。 */
    private Integer recommendation;

    /** 录用建议：STRONG_HIRE / HIRE / CONSIDER / REJECT。 */
    private String suggestion;

    /** AI分析摘要。 */
    private String summary;

    /** 候选人优势。 */
    private List<String> strengths;

    /** 候选人不足。 */
    private List<String> weaknesses;

    /** 雷达图维度数组（供前端Chart.js直接使用）。 */
    private List<DimensionVO> dimensions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private Integer score;
        private Integer maxScore = 100;
    }
}
