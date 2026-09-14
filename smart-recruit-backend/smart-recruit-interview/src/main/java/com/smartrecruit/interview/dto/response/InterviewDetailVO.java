package com.smartrecruit.interview.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 详细的面试视图，包含AI问题、反馈和分析。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InterviewDetailVO {

    /** 面试 ID。 */
    private Long id;
    /** 申请 ID。 */
    private Long applicationId;
    /** 候选人 ID。 */
    private Long candidateId;
    /** 职位 ID。 */
    private Long jobPositionId;
    /** 面试轮次。 */
    private Integer round;
    /** 面试类型。 */
    private Integer type;
    /** 面试主题。 */
    private String subject;
    /** 计划面试时间。 */
    private LocalDateTime scheduledTime;
    /** 面试时长（分钟）。 */
    private Integer durationMinutes;
    /** 面试地点。 */
    private String location;
    /** 面试状态。 */
    private Integer status;
    /** 面试结果。 */
    private Integer result;
    /** 面试官反馈文本。 */
    private String feedback;
    /** 综合评分。 */
    private Integer score;
    /** AI 评估详情（JSON）。 */
    private Object evaluation;
    /** AI 生成的面试题列表。 */
    private List<String> aiQuestions;
    /** AI 出题状态：0=模板/未生成, 1=生成中, 2=生成成功, 3=生成失败。 */
    private Integer aiQuestionStatus;
    /** AI 面试分析结果（JSON）。 */
    private Object aiAnalysis;
    /** 面试录制文件 URL。 */
    private String recordingUrl;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
}
