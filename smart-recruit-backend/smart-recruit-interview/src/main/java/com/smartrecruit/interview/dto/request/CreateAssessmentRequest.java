package com.smartrecruit.interview.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建在线测评请求 DTO。
 *
 * @since 1.0.0
 */
@Data
public class CreateAssessmentRequest {

    /** 候选人 ID。 */
    @NotNull(message = "候选人ID不能为空")
    @JsonProperty("candidateId")
    private Long candidateId;

    /** 候选人姓名。 */
    @JsonProperty("candidateName")
    private String candidateName;

    /** 应聘职位。 */
    @JsonProperty("jobTitle")
    private String jobTitle;

    /** 测评类型：0=编程测试,1=性格测试,2=智商测试。 */
    @NotNull(message = "测评类型不能为空")
    private Integer type;

    /** 候选人邮箱。 */
    @JsonProperty("candidateEmail")
    private String candidateEmail;

    /** 关联面试 ID（可选）。 */
    @JsonProperty("interviewId")
    private Long interviewId;
}
