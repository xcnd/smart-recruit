package com.smartrecruit.aiengine.controller;

import com.smartrecruit.aiengine.dto.request.TokenUsageReport;
import com.smartrecruit.aiengine.dto.request.TaskReport;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 智能体指标内部上报接口。
 *
 * <p>供其他微服务在完成真实大模型调用后，将按 Agent 消耗的 Token 数
 * 上报到 AI 引擎，统一计入「Token 消耗分布（近 24h）」等监控指标。</p>
 *
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/api/v1/agents/internal")
@RequiredArgsConstructor
@Slf4j
public class AgentMetricController {

    private final AgentTaskRecorder taskRecorder;

    /**
     * 上报一次真实 Token 消耗（按 Agent 累加到 24 小时指标）。
     *
     * @param report Token 用量上报信息
     * @return 统一成功响应
     */
    @PostMapping("/token-usage")
    public ApiResponse<Void> recordTokenUsage(@RequestBody TokenUsageReport report) {
        taskRecorder.recordTokenUsage(
                report.getAgentName(), report.getModelName(), report.getTokens());
        return ApiResponse.success(null);
    }

    /**
     * 上报一次 Agent 任务（供其他微服务在本地兜底逻辑中也记录任务/指标）。
     *
     * @param report 任务上报信息
     * @return 统一成功响应
     */
    @PostMapping("/task")
    public ApiResponse<Void> recordTask(@RequestBody TaskReport report) {
        taskRecorder.record(
                report.getAgentName(),
                report.getTaskType() == null ? 0 : report.getTaskType(),
                report.getInput(),
                report.getOutput(),
                !Boolean.FALSE.equals(report.getSuccess()),
                report.getDurationMs() == null ? 0L : report.getDurationMs(),
                report.getError());
        return ApiResponse.success(null);
    }
}
