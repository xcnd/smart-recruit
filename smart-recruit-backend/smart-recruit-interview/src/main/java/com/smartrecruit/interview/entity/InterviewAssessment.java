package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 面试评估实体，映射 {@code rec_interview_assessment} 表。
 *
 * <p>存储已完成面试轮次的结构化多维度评估分数和详细反馈，包括 AI 生成的分析。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName(value = "rec_interview_assessment", autoResultMap = true)
public class InterviewAssessment implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联面试记录的面试 ID。 */
    private Long interviewId;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 技术深度评分。 */
    private BigDecimal technologyDepth;

    /** 沟通能力评分。 */
    private BigDecimal communication;

    /** 问题解决能力评分。 */
    private BigDecimal problemSolving;

    /** 学习能力评分。 */
    private BigDecimal learningAbility;

    /** 团队协作评分。 */
    private BigDecimal teamwork;

    /** 综合评估得分。 */
    private BigDecimal overallScore;

    /** 综合评价文本。 */
    private String overallComment;

    /** 候选人优势（JSON 数组）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object strengths;

    /** 候选人不足或待改进项（JSON 数组）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object weaknesses;

    /** 面试关键节点（JSON 数组）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object keyMoments;

    /** 录用建议：HIRE、CONSIDER、REJECT。 */
    private Integer suggestion;

    /** 是否为 AI 生成（1 = 是，0 = 否）。 */
    private Integer aiGenerated;

    /** 评估人用户 ID。 */
    private Long assessorId;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
