package com.smartrecruit.aiengine.config;

import com.smartrecruit.aiengine.agents.v2.InterviewEvaluatorAgentV2;
import com.smartrecruit.aiengine.orchestration.InterviewOrchestrationAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AgentScope 运行时生命周期管理。
 *
 * <p>负责按 Agent 重建 AgentScope HarnessAgent 实例，使「重启」操作
 * 真正重新加载运行时、子 Agent 与配置，而不是只改状态。</p>
 *
 * @since 2026-04-12
 */
@Component
@ConditionalOnProperty(name = "agentscope.enabled", havingValue = "true")
@Slf4j
public class AgentScopeLifecycleManager {

    private final ObjectProvider<InterviewEvaluatorAgentV2> evaluatorV2Provider;
    private final ObjectProvider<InterviewOrchestrationAgent> orchestratorProvider;

    public AgentScopeLifecycleManager(
            ObjectProvider<InterviewEvaluatorAgentV2> evaluatorV2Provider,
            ObjectProvider<InterviewOrchestrationAgent> orchestratorProvider) {
        this.evaluatorV2Provider = evaluatorV2Provider;
        this.orchestratorProvider = orchestratorProvider;
    }

    /**
     * 重建指定 Agent 的 AgentScope 实例。
     *
     * @param agentId Agent ID（如 interview-evaluator / interview-orchestrator）
     * @return true 表示该 Agent 由 AgentScope 管理且已重建；false 表示无对应实例
     */
    public boolean rebuild(String agentId) {
        if (agentId == null) {
            return false;
        }
        try {
            return switch (agentId) {
                case "interview-evaluator" -> {
                    InterviewEvaluatorAgentV2 v2 = evaluatorV2Provider.getIfAvailable();
                    if (v2 != null) {
                        v2.rebuild();
                        log.info("AgentScope 实例已重建: agentId={}", agentId);
                        yield true;
                    }
                    yield false;
                }
                case "interview-orchestrator" -> {
                    InterviewOrchestrationAgent orch = orchestratorProvider.getIfAvailable();
                    if (orch != null) {
                        orch.rebuild();
                        log.info("AgentScope 实例已重建: agentId={}", agentId);
                        yield true;
                    }
                    yield false;
                }
                default -> false;
            };
        } catch (Exception e) {
            log.error("AgentScope 实例重建失败: agentId={}", agentId, e);
            throw new IllegalStateException("AgentScope 实例重建失败：" + e.getMessage(), e);
        }
    }
}
