package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测评提交结果，返回给候选人展示成绩。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AssessResultVO {

    @JsonProperty("assessmentId")
    private Long assessmentId;

    @JsonProperty("candidateName")
    private String candidateName;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("type")
    private Integer type;

    @JsonProperty("typeLabel")
    private String typeLabel;

    @JsonProperty("score")
    private String score;

    @JsonProperty("totalQuestions")
    private Integer totalQuestions;

    @JsonProperty("correctCount")
    private Integer correctCount;

    @JsonProperty("resultDescription")
    private String resultDescription;

    @JsonProperty("status")
    private Integer status;
}
