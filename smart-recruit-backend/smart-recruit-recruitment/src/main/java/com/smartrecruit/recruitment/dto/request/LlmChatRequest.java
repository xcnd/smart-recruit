package com.smartrecruit.recruitment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 引擎 LLM 对话请求。
 *
 * <p>业务模块不直接连接大模型厂商，统一通过 AI 引擎的 LLM 网关发起调用，
 * 由 AI 引擎完成模型路由、Token 统计与失败降级。</p>
 *
 * @since 2026-04-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatRequest {

    /** 系统提示词。 */
    private String systemPrompt;

    /** 用户提示词。 */
    private String userPrompt;

    /** Agent ID（如 smart-screener、resume-parser），AI 引擎按此路由模型并统计 Token。 */
    private String agentId;

    /** 归属的简历 ID，简历解析调用时用于 AI 引擎写入解析日志。 */
    private Long resumeId;

    /** 不归属具体简历的调用（如简历筛选）使用。 */
    public LlmChatRequest(String systemPrompt, String userPrompt, String agentId) {
        this(systemPrompt, userPrompt, agentId, null);
    }
}
