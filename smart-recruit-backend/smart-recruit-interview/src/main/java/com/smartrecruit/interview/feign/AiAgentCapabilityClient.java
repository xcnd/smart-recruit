package com.smartrecruit.interview.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

/**
 * AI 引擎 Agent 能力网关客户端（面试模块）。
 *
 * @since 2026-04-06
 */
@HttpExchange("/api/v1/agent-capabilities")
public interface AiAgentCapabilityClient {

    /** 生成面试题。 */
    @PostExchange("/interview/questions")
    ApiResponse<List<Map<String, Object>>> generateInterviewQuestions(
            @RequestBody com.smartrecruit.interview.dto.remote.AgentQuestionRequest request);

    /**
     * 异步提交 AI 出题任务，立即返回 taskId（避免 LLM 生成耗时导致 HTTP 超时）。
     */
    @PostExchange("/interview/questions/async")
    ApiResponse<Map<String, String>> submitInterviewQuestions(
            @RequestBody com.smartrecruit.interview.dto.remote.AgentQuestionRequest request);

    /**
     * 查询异步出题任务状态与结果（前端/业务侧轮询）。
     */
    @GetExchange("/interview/questions/async/{taskId}")
    ApiResponse<Map<String, Object>> getInterviewQuestionsTask(
            @PathVariable("taskId") String taskId);
}
