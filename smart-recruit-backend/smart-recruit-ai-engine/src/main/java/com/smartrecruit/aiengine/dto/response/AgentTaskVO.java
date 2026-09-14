package com.smartrecruit.aiengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI智能体任务队列条目VO（对齐前端 TaskQueueItemVO 数据格式）。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务 ID（前端使用 String 类型）。
     */
    private String id;
    /**
     * 智能体 ID。
     */
    private String agentId;
    /**
     * 智能体名称。
     */
    private String agentName;
    /**
     * 任务类型（前端字段名 type，人类可读字符串）。
     */
    private String type;
    /**
     * 优先级字符串：HIGH / MEDIUM / LOW。
     */
    private String priority;
    /**
     * 任务状态字符串：QUEUED / PROCESSING / COMPLETED / FAILED。
     */
    private String status;
    /**
     * 创建时间。
     */
    private LocalDateTime createdAt;
    /**
     * 开始执行时间。
     */
    private LocalDateTime startedAt;
    /**
     * 执行耗时（毫秒）。
     */
    private Long durationMs;
}
