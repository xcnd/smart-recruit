package com.smartrecruit.aiengine.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "ai_event_log", autoResultMap = true)
public class AiEventLog implements Serializable {

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
     * 关联的任务 ID。
     */
    private Long taskId;

    /**
     * 事件类型：START、PROGRESS、COMPLETE、ERROR 等。
     */
    private Integer eventType;

    /**
     * 事件来源，如 SSE、Webhook。
     */
    private Integer eventSource;

    /**
     * 事件标题，用于前端展示。
     */
    private String title;

    /**
     * 事件详细消息内容。
     */
    private String message;

    /**
     * 事件附加数据（JSON 格式）。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Object data;

    /**
     * 分布式链路追踪 ID。
     */
    private String traceId;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
