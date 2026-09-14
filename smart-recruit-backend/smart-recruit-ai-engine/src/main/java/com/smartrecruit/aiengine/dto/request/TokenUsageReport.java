package com.smartrecruit.aiengine.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 用量上报请求。
 *
 * <p>供其他微服务（如招聘模块的本地 LLM 网关）在完成真实调用后，
 * 将按 Agent 消耗的 Token 数上报给 AI 引擎，统一计入智能体监控指标。</p>
 *
 * @since 2026-04-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenUsageReport {

    /** Agent ID（如 resume-parser、smart-screener）。 */
    private String agentName;

    /** 实际使用的模型名（如 qwen3-max、deepseek-v4-flash）。 */
    private String modelName;

    /** 本次调用消耗的总 Token 数。 */
    private long tokens;
}
