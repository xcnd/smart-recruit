package com.smartrecruit.aiengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * AI智能体详细信息VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 智能体 ID。
     */
    private String agentId;
    /**
     * 智能体名称。
     */
    private String name;
    /**
     * 智能体功能描述。
     */
    private String description;
    /**
     * 使用的 AI 模型。
     */
    private String model;
    /**
     * 健康度（0-100）。
     */
    private String health;
    /**
     * 运行状态：ACTIVE、IDLE、ERROR。
     */
    private Integer status;
    /**
     * 智能体配置参数（JSON）。
     */
    private Map<String, Object> config;
    /**
     * 每小时调用量。
     */
    private Integer callsPerHour;
    /**
     * 平均延迟（毫秒）。
     */
    private Integer latencyMs;
    /**
     * 成功率（0-100）。
     */
    private Double successRate;
    /**
     * 累计任务总数。
     */
    private Long totalTasks;
    /**
     * 当前活跃任务数。
     */
    private Long activeTasks;
}
