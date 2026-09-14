package com.smartrecruit.interview.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 面试反馈表提交请求 DTO，包含五维星评、评语和录用建议。
 *
 * @since 1.0.0
 */
@Data
public class InterviewFeedbackRequest {

    /** 候选人 ID。 */
    @NotNull(message = "候选人ID不能为空")
    @JsonProperty("candidateId")
    private Long candidateId;

    /** 技术深度评分（1-5）。 */
    @Min(1) @Max(5)
    @JsonProperty("techRating")
    private Integer techRating;

    /** 沟通表达评分（1-5）。 */
    @Min(1) @Max(5)
    @JsonProperty("commRating")
    private Integer commRating;

    /** 问题解决评分（1-5）。 */
    @Min(1) @Max(5)
    @JsonProperty("solveRating")
    private Integer solveRating;

    /** 学习能力评分（1-5）。 */
    @Min(1) @Max(5)
    @JsonProperty("learnRating")
    private Integer learnRating;

    /** 团队协作评分（1-5）。 */
    @Min(1) @Max(5)
    @JsonProperty("teamRating")
    private Integer teamRating;

    /** 面试评语。 */
    @NotBlank(message = "面试评语不能为空")
    @JsonProperty("comments")
    private String comments;

    /** 录用建议：0=强烈推荐,1=推荐,2=待定,3=不推荐。 */
    @NotNull(message = "综合评价不能为空")
    @JsonProperty("hireRecommendation")
    private Integer hireRecommendation;

    /** 期望最低月薪（元）。 */
    @JsonProperty("expectedSalaryMin")
    private Integer expectedSalaryMin;

    /** 期望最高月薪（元）。 */
    @JsonProperty("expectedSalaryMax")
    private Integer expectedSalaryMax;

    /** 预计可入职日期（yyyy-MM-dd）。 */
    @JsonProperty("availableDate")
    private String availableDate;

    /** 特殊要求（远程办公、弹性工作、福利等）。 */
    @JsonProperty("specialRequirements")
    private String specialRequirements;
}
