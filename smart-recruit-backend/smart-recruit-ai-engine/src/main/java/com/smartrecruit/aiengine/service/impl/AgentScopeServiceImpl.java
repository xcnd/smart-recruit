package com.smartrecruit.aiengine.service.impl;

import com.smartrecruit.aiengine.agents.v2.InterviewEvaluatorAgentV2;
import com.smartrecruit.aiengine.dto.response.AgentScopeHealthVO;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.orchestration.InterviewOrchestrationAgent;
import com.smartrecruit.aiengine.service.AgentScopeService;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.common.dto.agentscope.EvaluateRequest;
import com.smartrecruit.common.dto.agentscope.EvaluateResponse;
import com.smartrecruit.common.dto.agentscope.OrchestrateRequest;
import com.smartrecruit.common.dto.agentscope.OrchestrateResponse;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * {@link AgentScopeService} 实现类。
 *
 * <p>封装 AgentScope2 HarnessAgent 面试评估（同步/流式）与多子 Agent 编排逻辑，
 * 统一记录 Agent 任务指标；Agent 未启用时返回明确的错误响应而不是抛异常。</p>
 *
 * @since 2026-04-12
 */
@Service
@Slf4j
public class AgentScopeServiceImpl implements AgentScopeService {

    /** 编排流水线各阶段名称（与 InterviewOrchestrationAgent 子 Agent 保持一致）。 */
    private static final String ORCHESTRATE_STAGES =
            "candidate-analyzer,question-generator,answer-evaluator,final-scorer";

    private final InterviewEvaluatorAgentV2 evaluatorV2;
    private final InterviewOrchestrationAgent orchestrator;
    private final AgentTaskRecorder taskRecorder;
    private final ObjectMapper objectMapper;

    public AgentScopeServiceImpl(
            @Autowired(required = false) InterviewEvaluatorAgentV2 evaluatorV2,
            @Autowired(required = false) InterviewOrchestrationAgent orchestrator,
            AgentTaskRecorder taskRecorder,
            ObjectMapper objectMapper) {
        this.evaluatorV2 = evaluatorV2;
        this.orchestrator = orchestrator;
        this.taskRecorder = taskRecorder;
        this.objectMapper = objectMapper;
    }

    /** HarnessAgent 面试评估（同步）。 */
    @Override
    public Mono<EvaluateResponse> evaluate(EvaluateRequest request) {
        log.info("[AgentScope2服务] 收到面试评估请求: candidateName={}", request.getCandidateName());
        long start = DateUtils.currentEpochMillis();

        if (evaluatorV2 == null) {
            log.warn("[AgentScope2服务] 面试评估 Agent 未启用(agentscope.sub-agents.interview-evaluator.enabled=false)");
            return Mono.just(EvaluateResponse.error(request.getSessionId(),
                    "AgentScope2 面试评估 Agent 未启用"))
                    .doOnSuccess(resp -> taskRecorder.record(
                            "interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), "", false, 0, resp.getError()));
        }

        return evaluatorV2.evaluate(request.getCandidateName(), request.getJobTitle(),
                        request.getInterviewContent(), request.getSessionId())
                .map(msg -> {
                    taskRecorder.record(
                            "interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), msg.getTextContent(), true,
                            DateUtils.currentEpochMillis() - start, null);
                    return EvaluateResponse.ok(request.getSessionId(), msg.getTextContent());
                })
                .onErrorResume(e -> {
                    log.error("[AgentScope2服务] 评估失败", e);
                    String message = "评估失败: " + e.getMessage();
                    taskRecorder.record(
                            "interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), "", false,
                            DateUtils.currentEpochMillis() - start, message);
                    return Mono.just(EvaluateResponse.error(request.getSessionId(), message));
                });
    }

    /** HarnessAgent 面试评估（SSE 流式）。 */
    @Override
    public Flux<String> evaluateStream(EvaluateRequest request) {
        log.info("[AgentScope2服务] 收到流式评估请求: candidateName={}", request.getCandidateName());
        long start = DateUtils.currentEpochMillis();

        if (evaluatorV2 == null) {
            return Flux.just("data: {\"error\":\"AgentScope2 面试评估 Agent 未启用\"}\n\n");
        }

        return evaluatorV2.evaluateStream(request.getCandidateName(), request.getJobTitle(),
                        request.getInterviewContent(), request.getSessionId())
                .map(event -> "data: " + event + "\n\n")
                .startWith("data: {\"status\":\"started\",\"sessionId\":\""
                        + request.getSessionId() + "\"}\n\n")
                .concatWithValues("data: {\"status\":\"completed\"}\n\n")
                .doOnComplete(() -> taskRecorder.record(
                        "interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                        toJson(request), "stream completed", true,
                        DateUtils.currentEpochMillis() - start, null))
                .onErrorResume(e -> {
                    taskRecorder.record(
                            "interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), "", false,
                            DateUtils.currentEpochMillis() - start, e.getMessage());
                    return Flux.just("data: {\"error\":\"" + e.getMessage() + "\"}\n\n");
                });
    }

    /** 子 Agent 编排面试评估。 */
    @Override
    public Mono<OrchestrateResponse> orchestrate(OrchestrateRequest request) {
        log.info("[AgentScope2服务] 收到编排请求: candidateName={}", request.getCandidateName());
        long start = DateUtils.currentEpochMillis();

        if (orchestrator == null) {
            return Mono.just(OrchestrateResponse.error(request.getSessionId(),
                    "AgentScope2 面试编排 Agent 未启用"))
                    .doOnSuccess(resp -> taskRecorder.record(
                            "orchestrator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), "", false, 0, resp.getError()));
        }

        return orchestrator.orchestrate(request.getCandidateName(), request.getCandidateProfile(),
                        request.getJobTitle(), request.getJobRequirements(), request.getSessionId())
                .map(msg -> {
                    taskRecorder.record(
                            "orchestrator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), msg.getTextContent(), true,
                            DateUtils.currentEpochMillis() - start, null);
                    return OrchestrateResponse.ok(request.getSessionId(),
                            msg.getTextContent(), ORCHESTRATE_STAGES);
                })
                .onErrorResume(e -> {
                    log.error("[AgentScope2服务] 编排失败", e);
                    String message = "编排失败: " + e.getMessage();
                    taskRecorder.record(
                            "orchestrator", AiEnums.TaskType.EVALUATE.getCode(),
                            toJson(request), "", false,
                            DateUtils.currentEpochMillis() - start, message);
                    return Mono.just(OrchestrateResponse.error(request.getSessionId(), message));
                });
    }

    /** AgentScope2 健康检查。 */
    @Override
    public Mono<AgentScopeHealthVO> health() {
        AgentScopeHealthVO status =
                new AgentScopeHealthVO(true, evaluatorV2 != null, orchestrator != null);
        return Mono.just(status);
    }

    /** 将请求体序列化为输入摘要。 */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
