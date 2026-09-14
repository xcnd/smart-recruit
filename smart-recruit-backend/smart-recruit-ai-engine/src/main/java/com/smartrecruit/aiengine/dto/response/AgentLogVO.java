package com.smartrecruit.aiengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI智能体执行日志条目VO。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日志 ID。
     */
    private Long id;
    /**
     * 智能体 ID。
     */
    private String agentId;
    /**
     * 智能体名称。
     */
    private String agentName;
    /**
     * 任务类型。
     */
    private Integer taskType;
    /**
     * 执行状态：PENDING、RUNNING、COMPLETED、FAILED。
     */
    private Integer status;
    /**
     * 执行耗时（毫秒）。
     */
    private Long durationMs;
    /**
     * 输入数据摘要。
     */
    private String inputSummary;
    /**
     * 输出结果摘要。
     */
    private String outputSummary;
    /**
     * 开始执行时间。
     */
    private LocalDateTime startedAt;
    /**
     * 完成时间。
     */
    private LocalDateTime completedAt;
}
