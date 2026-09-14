package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 评估报告详情 VO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentResponse {

    private Long id;
    private Long interviewId;
    private Long candidateId;
    private String candidateName;
    private String jobTitle;

    /** 五维评分（1.0-5.0）。 */
    private BigDecimal technologyDepth;
    private BigDecimal communication;
    private BigDecimal problemSolving;
    private BigDecimal learningAbility;
    private BigDecimal teamwork;

    /** 综合评分（0-100）。 */
    private BigDecimal overallScore;

    /** 综合评价文本。 */
    @JsonProperty("overallComment")
    private String overallComment;

    /** 优势列表。 */
    private List<String> strengths;

    /** 不足列表。 */
    private List<String> weaknesses;

    /** 关键时刻（带时间戳的事件列表）。 */
    private List<KeyMomentItem> keyMoments;

    /** 录用建议编码。 */
    private Integer suggestion;

    /** 录用建议标签。 */
    private String suggestionLabel;

    /** 是否 AI 生成。 */
    private boolean aiGenerated;

    /** 评估人 ID。 */
    private Long assessorId;

    /** 创建时间。 */
    private LocalDateTime createTime;

    /**
     * 关键时刻项。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyMomentItem {
        private String time;
        private String text;
    }
}
