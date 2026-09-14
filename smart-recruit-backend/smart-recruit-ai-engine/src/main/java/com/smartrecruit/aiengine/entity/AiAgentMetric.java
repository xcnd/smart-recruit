package com.smartrecruit.aiengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_agent_metric")
public class AiAgentMetric implements Serializable {

    /** 序列化版本号。 */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID。
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * AI 智能体名称。
     */
    private String agentName;

    /**
     * 指标采集时间点。
     */
    private LocalDateTime metricTime;

    /**
     * 每小时调用次数。
     */
    private BigDecimal callsPerHour;

    /**
     * 平均响应延迟（毫秒）。
     */
    private BigDecimal avgLatencyMs;

    /**
     * 准确率百分比（0-100）。
     */
    private BigDecimal accuracyPct;

    /**
     * 健康度百分比（0-100），综合评估智能体运行状态。
     */
    private BigDecimal healthPct;

    /**
     * 近 24 小时 Token 消耗总量。
     */
    @TableField("tokens_24h")
    private Long tokens24h;

    /**
     * 成功调用次数。
     */
    private Integer successCount;

    /**
     * 失败调用次数。
     */
    private Integer failCount;

    /**
     * 累计调用总次数。
     */
    private Integer totalCalls;

    /**
     * 使用的 AI 模型名称。
     */
    private String modelName;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
