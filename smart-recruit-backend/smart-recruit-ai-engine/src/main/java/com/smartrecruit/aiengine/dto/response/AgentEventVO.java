package com.smartrecruit.aiengine.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

/**
 * AI智能体流式事件（SSE）VO（对齐前端 AgentEventVO 数据格式）。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentEventVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事件 ID。
     */
    private String eventId;
    /**
     * 智能体 ID。
     */
    private String agentId;
    /**
     * 智能体名称。
     */
    private String agentName;
    /**
     * 事件类型数值（前端字段名 type）。
     */
    private Integer eventType;
    /**
     * 事件消息。
     */
    private String message;
    /**
     * 事件关联的执行状态。
     */
    private Integer status;
    /**
     * 事件时间戳。
     */
    private Instant timestamp;
    /**
     * 前端兼容字段：事件类型数值（与 eventType 相同）。
     */
    private Integer type;
    /**
     * 前端兼容字段：事件携带数据。
     */
    private Map<String, Object> data;
}
