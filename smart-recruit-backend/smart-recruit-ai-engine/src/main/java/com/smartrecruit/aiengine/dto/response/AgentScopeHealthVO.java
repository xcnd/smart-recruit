package com.smartrecruit.aiengine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * AgentScope2 框架健康检查视图对象。
 *
 * @param enabled      AgentScope2 是否启用
 * @param evaluatorV2  面试评估 Agent（V2）是否就绪
 * @param orchestrator 多 Agent 编排器是否就绪
 * @since 2026-04-07
 */
@JsonPropertyOrder({"enabled", "evaluatorV2", "orchestrator"})
public record AgentScopeHealthVO(
        @JsonProperty("enabled") boolean enabled,
        @JsonProperty("evaluatorV2") boolean evaluatorV2,
        @JsonProperty("orchestrator") boolean orchestrator
) {
}
