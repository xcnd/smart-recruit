package com.smartrecruit.aiengine.service;

import com.smartrecruit.aiengine.dto.request.LlmChatRequest;

import java.util.Map;

/**
 * LLM 网关门面服务。
 *
 * <p>封装通用 LLM 对话的完整调用链路（可用性校验、Agent 任务生命周期记录、
 * 网关调用与耗时指标），供 Controller 层薄调用。</p>
 *
 * @since 2026-04-12
 */
public interface LlmFacadeService {

    /**
     * 发起一次通用 LLM 对话（供其他微服务通过 {@code @HttpExchange} 调用）。
     *
     * <p>内部会校验 LLM 服务是否启用，记录 Agent 任务生命周期
     * （执行中 → 完成/失败），并按 Agent 统计耗时指标。</p>
     *
     * @param request 系统提示词与用户提示词
     * @return 大模型原始返回结果
     */
    Map<String, Object> chat(LlmChatRequest request);
}
