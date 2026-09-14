package com.smartrecruit.recruitment.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.request.TaskReport;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * AI 引擎 Agent 任务上报客户端。
 *
 * <p>招聘模块在本地兜底逻辑（如 LLM 失败降级为启发式评分）中，
 * 也把简历筛选任务上报给 AI 引擎，保证任务队列始终有真实记录。</p>
 *
 * @since 2026-04-12
 */
@HttpExchange("/api/v1/agents/internal")
public interface AiAgentTaskClient {

    /**
     * 上报一次 Agent 任务。
     */
    @PostExchange("/task")
    ApiResponse<Void> reportTask(@RequestBody TaskReport report);
}
