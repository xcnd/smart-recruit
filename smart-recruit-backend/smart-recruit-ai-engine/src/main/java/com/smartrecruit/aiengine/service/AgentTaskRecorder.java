package com.smartrecruit.aiengine.service;

import com.smartrecruit.aiengine.enums.AiEnums;

/**
 * Agent 任务与指标记录器。
 *
 * <p>每次能力调用落一条任务记录，并滚动更新智能体指标
 * （成功/失败次数、平均延迟、成功率、健康度），供 AI 智能编排监控页读取真实数据。</p>
 *
 * @since 2026-04-06
 */
public interface AgentTaskRecorder {

    /**
     * 记录一次 Agent 能力调用。
     *
     * @param agentName  智能体名称（如 smart-screener）
     * @param taskType   任务类型（见 {@link AiEnums.TaskType}）
     * @param input      输入摘要
     * @param output     输出摘要
     * @param success    是否成功
     * @param durationMs 耗时（毫秒）
     * @param error      错误信息（失败时）
     */
    void record(String agentName, int taskType, String input, String output,
                boolean success, long durationMs, String error);

    /**
     * 开始一个真实任务（写入执行中状态，进入任务队列）。
     *
     * @param agentName 智能体名称（如 smart-screener）
     * @param taskType  任务类型（见 {@link AiEnums.TaskType}）
     * @param input     输入摘要
     * @return 任务 ID，用于 {@link #completeTask(Long, boolean, String, long, String)} 收尾
     */
    long startTask(String agentName, int taskType, String input);

    /**
     * 完成一个真实任务（更新状态、耗时并滚动更新指标）。
     *
     * @param taskId     任务 ID（由 {@link #startTask} 返回）
     * @param success    是否成功
     * @param output     输出摘要
     * @param durationMs 真实耗时（毫秒）
     * @param error      错误信息（失败时）
     */
    void completeTask(Long taskId, boolean success, String output, long durationMs, String error);

    /**
     * 记录一次真实的 Token 消耗（按 Agent 累加到 24 小时指标）。
     *
     * <p>由 LLM 网关在每次成功调用后上报，作为「Token 消耗分布（近 24h）」
     * 图表的数据来源。</p>
     *
     * @param agentName 智能体名称（如 offer-predictor）
     * @param modelName 实际使用的模型名（如 deepseek-v4-flash）
     * @param tokens    本次调用消耗的总 Token 数
     */
    void recordTokenUsage(String agentName, String modelName, long tokens);
}
