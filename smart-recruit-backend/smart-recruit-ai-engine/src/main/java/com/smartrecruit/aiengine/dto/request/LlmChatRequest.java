package com.smartrecruit.aiengine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LLM 对话请求 DTO。
 *
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest {
    private String systemPrompt;
    private String userPrompt;
    /** 可选：Agent ID（如 offer-predictor），网关按 Agent 路由模型。 */
    private String agentId;
}
