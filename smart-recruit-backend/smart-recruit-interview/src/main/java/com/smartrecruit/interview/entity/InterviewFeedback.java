package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试反馈实体，映射 {@code rec_interview_feedback} 表。
 *
 * <p>存储每位面试官针对某一次面试会话的详细维度级别反馈。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_interview_feedback")
public class InterviewFeedback implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的面试记录 ID。 */
    private Long interviewId;

    /** 提供此反馈的面试官用户 ID。 */
    private Long interviewerId;

    /** 技术深度评分（1.0-5.0）。 */
    private BigDecimal technologyDepth;

    /** 沟通表达评分（1.0-5.0）。 */
    private BigDecimal communication;

    /** 问题解决评分（1.0-5.0）。 */
    private BigDecimal problemSolving;

    /** 学习能力评分（1.0-5.0）。 */
    private BigDecimal learningAbility;

    /** 团队协作评分（1.0-5.0）。 */
    private BigDecimal teamwork;

    /** 综合评分（1-5）。 */
    private Integer overallRating;

    /** 维度评分（JSON）- 6 个维度的打分。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object dimensions;

    /** 候选人优势。 */
    private String strengths;

    /** 候选人不足。 */
    private String weaknesses;

    /** 面试官建议。 */
    private String suggestions;

    /** 综合评价。 */
    private String overallComment;

    /** 录用建议：0=不建议，1=建议录用，2=强烈推荐。 */
    private Integer hireRecommendation;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
