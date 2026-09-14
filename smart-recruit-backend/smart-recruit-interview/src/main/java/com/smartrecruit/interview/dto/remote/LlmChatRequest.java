package com.smartrecruit.interview.dto.remote;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 调用 LLM 网关对话请求（内部服务间调用）。
 *
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest {

    /** 系统提示词。 */
    private String systemPrompt;

    /** 用户提示词。 */
    private String userPrompt;

    /** Agent ID（如 interview-evaluator、interview-question），AI 引擎按此路由模型并统计 Token。 */
    private String agentId;
}
