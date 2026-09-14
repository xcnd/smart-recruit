package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 在线测评页面数据 — 通过测评 token 获取，返回给候选人展示。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentPageVO {

    @JsonProperty("assessmentId")
    private Long assessmentId;

    @JsonProperty("candidateId")
    private Long candidateId;

    @JsonProperty("candidateName")
    private String candidateName;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("typeLabel")
    private String typeLabel;

    @JsonProperty("status")
    private Integer status;

    @JsonProperty("candidateEmail")
    private String candidateEmail;

    @JsonProperty("sentTime")
    private String sentTime;

    @JsonProperty("questions")
    private List<AssessmentQuestionItem> questions;

    @JsonProperty("durationMinutes")
    private Integer durationMinutes;
}
