package com.smartrecruit.aiengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 面试评估调用日志实体，映射 {@code ai_interview_log} 表。
 *
 * @author xdh
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "ai_interview_log", autoResultMap = true)
public class AiInterviewLog implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的面试 ID，对应 rec_interview.id。
     */
    private Long interviewId;

    /**
     * AI 引擎名称，如 qwen、deepseek。
     */
    private String engine;

    /**
     * AI 模型名称。
     */
    private String model;

    /**
     * 评估类型：TECHNICAL、BEHAVIORAL、COMPREHENSIVE。
     */
    private Integer assessmentType;

    /**
     * 输入 Token 数量。
     */
    private Integer inputTokens;

    /**
     * 输出 Token 数量。
     */
    private Integer outputTokens;

    /**
     * Token 消耗总量（输入 + 输出）。
     */
    private Integer totalTokens;

    /**
     * API 调用耗时（毫秒）。
     */
    private Long durationMs;

    /**
     * 本次调用费用（美元）。
     */
    private BigDecimal costUsd;

    /**
     * 评估结果（JSON 格式），包含分维度评分。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object resultJson;

    /**
     * 评估状态：SUCCESS、FAILED、PARTIAL。
     */
    private Integer status;

    /**
     * 错误信息，成功时为空。
     */
    private String errorMsg;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
