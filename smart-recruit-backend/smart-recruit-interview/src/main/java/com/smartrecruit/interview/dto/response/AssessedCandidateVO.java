package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 已评估候选人的简要信息 — 用于 AI 评估报告下拉列表。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessedCandidateVO {

    @JsonProperty("interviewId")
    private Long interviewId;

    @JsonProperty("candidateId")
    private Long candidateId;

    @JsonProperty("departmentId")
    private Long departmentId;

    @JsonProperty("candidateName")
    private String candidateName;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("overallScore")
    private BigDecimal overallScore;

    @JsonProperty("assessedAt")
    private String assessedAt;
}
