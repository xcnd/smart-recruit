package com.smartrecruit.interview.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 面试列表/展示视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewVO {

    /** 面试 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 申请 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** 职位 ID。 */
    @JsonProperty("jobId")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPositionId;
    /** 职位名称。 */
    private String jobTitle;
    /** 面试官 ID 列表（JSON 数组）。 */
    @JsonProperty("interviewerIds")
    private Object interviewerIds;
    /** 面试官姓名。 */
    private String interviewerName;
    /** 面试轮次。 */
    private Integer round;
    /** 是否为终面：0=否, 1=是。 */
    @JsonProperty("isFinalRound")
    private Integer isFinalRound;
    /** 面试类型。 */
    private Integer type;
    /** 面试类型标签。 */
    private String typeLabel;
    /** 面试主题。 */
    private String subject;
    /** 计划面试时间。 */
    @JsonProperty("scheduledAt")
    private LocalDateTime scheduledTime;
    /** 面试时长（分钟）。 */
    @JsonProperty("duration")
    private Integer durationMinutes;
    /** 面试地点或视频会议链接。 */
    private String location;
    /** 面试状态。 */
    private Integer status;
    /** 面试状态标签。 */
    private String statusLabel;
    /** 面试结果。 */
    private Integer result;
    /** 面试结果标签。 */
    private String resultLabel;
    /** 综合评分（0-100）。 */
    private Integer score;
    /** AI 出题状态：0=模板/未生成, 1=生成中, 2=生成成功, 3=生成失败。 */
    private Integer aiQuestionStatus;
    /** 创建时间。 */
    private LocalDateTime createTime;
}
