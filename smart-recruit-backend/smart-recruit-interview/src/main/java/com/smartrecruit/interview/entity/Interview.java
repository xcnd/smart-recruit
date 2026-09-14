package com.smartrecruit.interview.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * 面试实体，映射到 {@code rec_interview} 表。
 *
 * <p>跟踪每个职位申请的每一轮面试，包括排期、
 * 面试类型、AI 生成的问题以及最终结果。</p>
 *
 * @since 1.0.0
 */
@Data
@TableName("rec_interview")
public class Interview implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 申请 ID，关联候选人与职位。 */
    private Long applicationId;

    /** 候选人 ID。 */
    private Long candidateId;

    /** 候选人姓名（冗余字段，避免跨库查询）。 */
    private String candidateName;

    /** 职位 ID。 */
    private Long jobPositionId;

    /** 职位名称（冗余字段，避免跨库查询）。 */
    private String jobTitle;

    /** 面试轮次编号（1, 2, 3, ...）。 */
    private Integer round;

    /** 是否为终面：0=否, 1=是。终面完成后不再安排后续面试。 */
    private Integer isFinalRound;

    /** 面试类型：AI, PHONE, VIDEO, ONSITE, TECHNICAL, HR, EXECUTIVE。 */
    private Integer type;

    /** 面试主题或标题。 */
    private String subject;

    /** 计划面试日期和时间。 */
    private LocalDateTime scheduledTime;

    /** 预计时长（分钟）。 */
    private Integer durationMinutes;

    /** 会议地点或链接。 */
    private String location;

    /** 面试官用户 ID（JSON 数组）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object interviewerIds = new ArrayList<>();

    /** 面试状态：SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, RESCHEDULED, NO_SHOW。 */
    private Integer status;

    /** 结果：PASS, FAIL, HOLD。 */
    private Integer result;

    /** 面试官反馈文本。 */
    private String feedback;

    /** 面试总评分（0-100）。 */
    private Integer score;

    /** 结构化评估（JSON）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object evaluation;

    /** AI评估状态: null=未评估, PROCESSING=评估中, COMPLETED=已完成, FAILED=失败 */
    @TableField("evaluation_status")
    private String evaluationStatus;

    /** AI 面试记录文本。 */
    private String aiTranscript;

    /** AI 出题状态：0=模板/未生成, 1=生成中, 2=生成成功, 3=生成失败（保留模板）。 */
    @TableField("ai_question_status")
    private Integer aiQuestionStatus;

    /** AI 分析结果（JSON）。 */
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private Object aiAnalysis;

    /** 创建时间。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 ID。 */
    private String createBy;

    /** 更新人 ID。 */
    private String updateBy;

    /** 逻辑删除标记：0 = 未删除，1 = 已删除。 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted = 0;
}
