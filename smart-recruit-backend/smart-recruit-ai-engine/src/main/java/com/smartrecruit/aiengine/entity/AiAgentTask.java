package com.smartrecruit.aiengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 映射到{@code ai_agent_task}表的AI智能体任务实体。
 *
 * <p>记录每个AI智能体执行的生命周期：
 * 排队中 -&gt; 执行中 -&gt; 已完成 / 失败。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@TableName("ai_agent_task")
public class AiAgentTask implements Serializable {

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
     * 任务类型：RESUME_PARSE、INTERVIEW_EVAL、OFFER_PREDICT 等。
     */
    private Integer taskType;

    /**
     * 任务优先级：0 = 低，1 = 中，2 = 高，3 = 紧急。
     */
    private Integer priority;

    /**
     * 任务输入数据（JSON 格式）。
     */
    private String inputData;

    /**
     * 任务输出结果（JSON 格式）。
     */
    private String outputData;

    /**
     * 任务状态：PENDING、RUNNING、COMPLETED、FAILED、CANCELLED。
     */
    private Integer status;

    /**
     * 任务开始执行时间。
     */
    private LocalDateTime startedAt;

    /**
     * 任务完成时间。
     */
    private LocalDateTime completedAt;

    /**
     * 任务执行耗时（毫秒）。
     */
    private Long durationMs;

    /**
     * 错误信息，任务成功时为空。
     */
    private String errorMessage;

    /**
     * 任务创建时间。
     */
    private LocalDateTime createdAt;
}
