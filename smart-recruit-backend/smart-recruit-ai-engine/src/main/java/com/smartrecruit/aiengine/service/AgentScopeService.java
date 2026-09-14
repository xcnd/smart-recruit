package com.smartrecruit.aiengine.service;

import com.smartrecruit.aiengine.dto.response.AgentScopeHealthVO;
import com.smartrecruit.common.dto.agentscope.EvaluateRequest;
import com.smartrecruit.common.dto.agentscope.EvaluateResponse;
import com.smartrecruit.common.dto.agentscope.OrchestrateRequest;
import com.smartrecruit.common.dto.agentscope.OrchestrateResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AgentScope2 面试评估与编排服务。
 *
 * <p>封装基于 AgentScope2 HarnessAgent 的面试评估（同步/SSE 流式）和
 * 多子 Agent 编排逻辑，统一记录 Agent 任务指标，供 Controller 层薄调用。</p>
 *
 * @since 2026-04-12
 */
public interface AgentScopeService {

    /**
     * HarnessAgent 面试评估（同步）。
     *
     * @param request 评估请求（候选人、职位、面试内容、会话 ID）
     * @return 评估结果
     */
    Mono<EvaluateResponse> evaluate(EvaluateRequest request);

    /**
     * HarnessAgent 面试评估（SSE 流式）。
     *
     * @param request 评估请求
     * @return SSE 事件流
     */
    Flux<String> evaluateStream(EvaluateRequest request);

    /**
     * 子 Agent 编排面试评估。
     *
     * @param request 编排请求（候选人、职位、需求、会话 ID）
     * @return 编排结果
     */
    Mono<OrchestrateResponse> orchestrate(OrchestrateRequest request);

    /**
     * AgentScope2 框架健康检查。
     *
     * @return 各组件启用状态
     */
    Mono<AgentScopeHealthVO> health();
}
