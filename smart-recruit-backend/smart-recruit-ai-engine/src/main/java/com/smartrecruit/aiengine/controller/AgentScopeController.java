package com.smartrecruit.aiengine.controller;

import com.smartrecruit.aiengine.dto.response.AgentScopeHealthVO;
import com.smartrecruit.aiengine.service.AgentScopeService;
import com.smartrecruit.common.dto.agentscope.EvaluateRequest;
import com.smartrecruit.common.dto.agentscope.EvaluateResponse;
import com.smartrecruit.common.dto.agentscope.OrchestrateRequest;
import com.smartrecruit.common.dto.agentscope.OrchestrateResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * AgentScope2 REST 端点。
 *
 * <p>提供基于 AgentScope2 HarnessAgent 的面试评估和编排接口，
 * 供 interview 模块通过 Feign 客户端调用。
 * 业务逻辑（Agent 调用、任务指标记录）统一委托 {@link AgentScopeService}。</p>
 *
 * @since 2.0.0
 */
@RestController
@RequestMapping("/api/v1/agentscope")
@ConditionalOnProperty(name = "agentscope.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class AgentScopeController {

    private final AgentScopeService agentScopeService;

    /**
     * HarnessAgent 面试评估（同步）。
     */
    @PostMapping("/interview/evaluate")
    public Mono<EvaluateResponse> evaluate(@RequestBody EvaluateRequest request) {
        return agentScopeService.evaluate(request);
    }

    /**
     * HarnessAgent 面试评估（SSE 流式）。
     *
     * <p>使用 Server-Sent Events 实时推送评估进度，前端可逐步展示评估结果。</p>
     */
    @PostMapping(value = "/interview/evaluate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> evaluateStream(@RequestBody EvaluateRequest request) {
        return agentScopeService.evaluateStream(request);
    }

    /**
     * 子 Agent 编排面试评估。
     */
    @PostMapping("/interview/orchestrate")
    public Mono<OrchestrateResponse> orchestrate(@RequestBody OrchestrateRequest request) {
        return agentScopeService.orchestrate(request);
    }

    /**
     * 健康检查端点。
     */
    @GetMapping("/health")
    public Mono<AgentScopeHealthVO> health() {
        return agentScopeService.health();
    }
}
