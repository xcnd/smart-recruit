package com.smartrecruit.aiengine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 任务上报请求（供其他微服务在本地兜底逻辑中也记录 Agent 任务）。
 *
 * @since 2026-04-12
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskReport {

    /** Agent ID（如 smart-screener）。 */
    private String agentName;

    /** 任务类型（见 AiEnums.TaskType，缺省 0）。 */
    private Integer taskType;

    /** 是否成功。 */
    private Boolean success;

    /** 耗时（毫秒）。 */
    private Long durationMs;

    /** 输入摘要。 */
    private String input;

    /** 输出摘要。 */
    private String output;

    /** 错误信息（失败时）。 */
    private String error;
}
