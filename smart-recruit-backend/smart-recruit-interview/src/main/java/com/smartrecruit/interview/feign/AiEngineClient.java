package com.smartrecruit.interview.feign;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.Map;

/**
 * AI Engine 微服务 HTTP 客户端 — LLM 网关调用。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1/llm")
public interface AiEngineClient {

    @PostExchange("/chat")
    Map<String, Object> chat(@RequestBody com.smartrecruit.interview.dto.remote.LlmChatRequest request);
}
