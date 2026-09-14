package com.smartrecruit.interview.feign;

import com.smartrecruit.common.dto.agentscope.EvaluateRequest;
import com.smartrecruit.common.dto.agentscope.EvaluateResponse;
import com.smartrecruit.common.dto.agentscope.OrchestrateRequest;
import com.smartrecruit.common.dto.agentscope.OrchestrateResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * AI Engine AgentScope2 端点 HTTP 客户端。
 *
 * <p>与 {@link AiEngineClient} 分开定义，因为 AgentScope2 端点使用不同的 base path
 * ({@code /api/v1/agentscope})。</p>
 *
 * @since 2.0.0
 */
@HttpExchange("/api/v1/agentscope")
public interface AiAgentScopeClient {

    /** AgentScope2 HarnessAgent 面试评估（同步）。 */
    @PostExchange("/interview/evaluate")
    EvaluateResponse evaluate(@RequestBody EvaluateRequest request);

    /** AgentScope2 多 Agent 编排面试评估（4 个子 Agent 协作）。 */
    @PostExchange("/interview/orchestrate")
    OrchestrateResponse orchestrate(@RequestBody OrchestrateRequest request);
}
