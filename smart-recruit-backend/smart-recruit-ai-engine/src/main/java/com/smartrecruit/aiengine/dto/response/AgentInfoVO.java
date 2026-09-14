package com.smartrecruit.aiengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * AI智能体摘要信息VO（对齐前端 AgentInfoVO 数据格式）。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 智能体唯一标识（前端字段名 id）。
     */
    private String id;
    /**
     * 智能体名称。
     */
    private String name;
    /**
     * 智能体职责描述。
     */
    private String description;
    /**
     * 智能体类型：0=ORCHESTRATION, 1=EXECUTION, 2=REVIEW。
     */
    private Integer type;
    /**
     * 运行状态：0=RUNNING, 1=IDLE, 2=PAUSED, 3=ERROR。
     */
    private Integer status;
    /**
     * 健康度（0-100）。
     */
    private Double health;
    /**
     * 关键指标。
     */
    private AgentMetricsVO metrics;
    /**
     * 运行配置。
     */
    private Map<String, Object> config;
    /**
     * 最后更新时间描述。
     */
    private String updatedAt;

    /**
     * Agent 关键指标子 VO。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentMetricsVO implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long tasksCompleted;
        private Integer avgResponseTime;
        private Double successRate;
        private Integer uptime;
    }
}
