package com.smartrecruit.interview.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 创建/更新 AI 评估报告的请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class AssessmentRequest {

    /** 候选人 ID。 */
    @NotNull(message = "候选人ID不能为空")
    private Long candidateId;

    /** 候选人姓名。 */
    @JsonProperty("candidateName")
    private String candidateName;

    /** 职位名称。 */
    @JsonProperty("jobTitle")
    private String jobTitle;

    /** 技术深度评分（1.0-5.0）。 */
    @Min(value = 0) @Max(value = 5)
    private BigDecimal technologyDepth;

    /** 沟通表达评分（1.0-5.0）。 */
    @Min(value = 0) @Max(value = 5)
    private BigDecimal communication;

    /** 问题解决评分（1.0-5.0）。 */
    @Min(value = 0) @Max(value = 5)
    private BigDecimal problemSolving;

    /** 学习能力评分（1.0-5.0）。 */
    @Min(value = 0) @Max(value = 5)
    private BigDecimal learningAbility;

    /** 团队协作评分（1.0-5.0）。 */
    @Min(value = 0) @Max(value = 5)
    private BigDecimal teamwork;

    /** 综合评分（0-100）。 */
    @Min(value = 0) @Max(value = 100)
    private BigDecimal overallScore;

    /** 综合评价文本。 */
    @JsonProperty("overallComment")
    private String overallComment;

    /** 优势标签（JSON 数组）。 */
    private List<String> strengths;

    /** 不足标签（JSON 数组）。 */
    private List<String> weaknesses;

    /** 关键时刻列表。 */
    private List<KeyMomentItem> keyMoments;

    /** 录用建议：0=强烈推荐,1=推荐,2=待定,3=不推荐,4=加试。 */
    private Integer suggestion;

    /**
     * 关键时刻项。
     */
    @Data
    public static class KeyMomentItem {
        private String time;
        private String text;
    }
}
