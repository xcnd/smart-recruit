package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 在线测评列表视图 VO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineAssessmentVO {

    private Long id;

    private Long interviewId;

    private Long candidateId;

    @JsonProperty("candidateName")
    private String candidateName;

    @JsonProperty("jobTitle")
    private String jobTitle;

    /** 测评类型编码（0/1/2）。 */
    private Integer type;

    /** 测评类型标签（如 "编程测试"）。 */
    @JsonProperty("typeLabel")
    private String typeLabel;

    /** 发送时间。 */
    @JsonProperty("sentTime")
    private LocalDateTime sentTime;

    /** 状态：0=未发送,1=待完成,2=已完成。 */
    private Integer status;

    /** 状态标签。 */
    @JsonProperty("statusLabel")
    private String statusLabel;

    /** 成绩。 */
    private String score;

    /** 候选人邮箱。 */
    @JsonProperty("candidateEmail")
    private String candidateEmail;

    /** 创建时间。 */
    @JsonProperty("createTime")
    private LocalDateTime createTime;
}
