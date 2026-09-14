package com.smartrecruit.recruitment.feign;

import com.smartrecruit.recruitment.dto.request.LlmChatRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * AI 引擎 LLM 网关客户端。
 *
 * <p>业务模块不直接连接大模型厂商，统一通过 AI 引擎的 LLM 网关发起调用，
 * 由 AI 引擎完成模型路由、Token 统计与失败降级。</p>
 *
 * @since 2026-04-11
 */
@HttpExchange("/api/v1/llm")
public interface AiAgentLlmClient {

    /**
     * 发起一次 LLM 对话（按 Agent 路由模型）。
     *
     * @param request 系统提示词、用户提示词与 Agent ID
     * @return 大模型返回的结构化结果
     */
    @PostExchange("/chat")
    Map<String, Object> chat(@RequestBody LlmChatRequest request);
}
