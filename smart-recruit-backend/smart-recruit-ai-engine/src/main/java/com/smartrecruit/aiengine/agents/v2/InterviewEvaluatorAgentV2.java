package com.smartrecruit.aiengine.agents.v2;

import com.smartrecruit.aiengine.config.AgentScopeProperties;
import com.smartrecruit.aiengine.config.AiEvaluationPrompts;
import com.smartrecruit.aiengine.service.LlmGatewayService;
import com.smartrecruit.common.util.DateUtils;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.*;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 面试评估 Agent V2 — 基于 AgentScope2 HarnessAgent 实现。
 *
 * <p>与 V1 版 {@code InterviewEvaluatorAgent} 共存，通过
 * {@code agentscope.sub-agents.interview-evaluator.enabled=true} 激活。
 * 使用 AgentScope2 的 HarnessAgent API 实现结构化面试评估，
 * 底层模型调用复用现有的 {@link LlmGatewayService}。</p>
 *
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(name = "agentscope.sub-agents.interview-evaluator.enabled", havingValue = "true")
@Slf4j
public class InterviewEvaluatorAgentV2 {

    private final LlmGatewayService llmGatewayService;
    private final AgentScopeProperties properties;
    /** 当前生效的 HarnessAgent 实例（重启时整体重建并替换）。 */
    private final AtomicReference<HarnessAgent> agentRef = new AtomicReference<>();

    public InterviewEvaluatorAgentV2(LlmGatewayService llmGatewayService,
                                      AgentScopeProperties properties) {
        this.llmGatewayService = llmGatewayService;
        this.properties = properties;
        this.agentRef.set(buildAgent());
        log.info("InterviewEvaluatorAgentV2 初始化完成: agentId={}",
                agentRef.get().getAgentId());
    }

    /**
     * 重建 AgentScope HarnessAgent 实例（重启时真正重新加载运行时与配置）。
     */
    public HarnessAgent rebuild() {
        HarnessAgent fresh = buildAgent();
        agentRef.set(fresh);
        log.info("InterviewEvaluatorAgentV2 已重建: agentId={}, model={}",
                fresh.getAgentId(), fresh.getModel().getModelName());
        return fresh;
    }

    private HarnessAgent buildAgent() {
        Model modelAdapter = new LlmGatewayModelAdapter(llmGatewayService);
        return HarnessAgent.builder()
                .name("interview-evaluator")
                .description("AI面试评估智能体，对候选人面试表现进行六维度评估")
                .sysPrompt(AiEvaluationPrompts.SYSTEM_PROMPT)
                .model(modelAdapter)
                .workspace(Paths.get(properties.getWorkspace().getPath()))
                .maxIters(3)
                .enableTaskList(false)
                .checkRunning(false)
                .build();
    }

    /**
     * 评估候选人的面试表现。
     *
     * @param candidateName  候选人姓名
     * @param jobTitle       应聘职位
     * @param interviewContent 面试内容（问题和回答摘要）
     * @param sessionId      会话标识
     * @return 评估结果的 Mono
     */
    public Mono<Msg> evaluate(String candidateName, String jobTitle,
                               String interviewContent, String sessionId) {
        String userPrompt = buildUserPrompt(candidateName, jobTitle, interviewContent);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("system")
                .build();

        UserMessage userMsg = new UserMessage(userPrompt);

        log.info("HarnessAgent 开始评估: candidateName={}, sessionId={}", candidateName, sessionId);
        return agentRef.get().call(userMsg, ctx)
                .doOnSuccess(result -> log.info("HarnessAgent 评估完成: sessionId={}, textLength={}",
                        sessionId, result.getTextContent() != null ? result.getTextContent().length() : 0))
                .doOnError(e -> log.error("HarnessAgent 评估失败: sessionId={}", sessionId, e));
    }

    /**
     * 以流式方式评估（适用于需要实时反馈的场景）。
     */
    public Flux<AgentEvent> evaluateStream(
            String candidateName, String jobTitle,
            String interviewContent, String sessionId) {
        String userPrompt = buildUserPrompt(candidateName, jobTitle, interviewContent);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("system")
                .build();

        UserMessage userMsg = new UserMessage(userPrompt);
        return agentRef.get().streamEvents(userMsg, ctx);
    }

    public HarnessAgent getAgent() {
        return agentRef.get();
    }

    // ================================================================
    // 私有方法
    // ================================================================

    private String buildUserPrompt(String candidateName, String jobTitle, String content) {
        return """
                ## 候选人信息
                - 姓名：%s
                - 应聘职位：%s

                ## 面试内容
                %s

                请根据以上面试内容，对候选人进行六维度专业评估，输出 JSON 格式结果。""".formatted(
                candidateName != null ? candidateName : "未知",
                jobTitle != null ? jobTitle : "未知",
                content != null ? content : "无面试内容");
    }

    /**
     * LangChain4j LlmGatewayService → AgentScope2 Model 接口适配器（内部实现）。
     */
    static class LlmGatewayModelAdapter implements Model {

        private final LlmGatewayService gateway;

        LlmGatewayModelAdapter(LlmGatewayService gateway) {
            this.gateway = gateway;
        }

        @Override
        public String getModelName() {
            return "smartrecruit-llm-gateway";
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages,
                List<ToolSchema> toolSchemas,
                GenerateOptions options) {

            String systemPrompt = extractSystemPrompt(messages);
            String userPrompt = extractUserPrompt(messages);

            // AgentScope2 HarnessAgent 可能不通过 SystemMessage 传递 sysPrompt，
            // 抽取失败时回退到预置的面试评估 System Prompt
            if (systemPrompt == null || systemPrompt.isBlank()) {
                systemPrompt = AiEvaluationPrompts.SYSTEM_PROMPT;
            }

            log.debug("[AgentScope2适配器] systemPrompt长度={}, userPrompt长度={}",
                    systemPrompt != null ? systemPrompt.length() : 0,
                    userPrompt != null ? userPrompt.length() : 0);

            try {
                // 取模型原始文本（可能为工具调用/自由文本），避免 JSON 解析破坏编排链路
                // 面试评估走 interview-evaluator 的路由模型（如 deepseek-v4）
                String textContent = gateway.chatRaw(systemPrompt, userPrompt, "interview-evaluator");

                ChatResponse response = ChatResponse.builder()
                                .id("as2-" + DateUtils.currentEpochMillis())
                                .content(List.of(
                                        TextBlock.builder()
                                                .text(textContent).build()))
                                .finishReason("stop")
                                .build();

                return Flux.just(response);
            } catch (Exception e) {
                log.error("[AgentScope2适配器] LLM调用失败", e);
                return Flux.error(e);
            }
        }

        private String extractSystemPrompt(List<Msg> messages) {
            if (messages == null) return "";
            for (Msg msg : messages) {
                if (msg instanceof SystemMessage) {
                    return msg.getTextContent();
                }
            }
            return "";
        }

        private String extractUserPrompt(List<Msg> messages) {
            if (messages == null) return "";
            for (int i = messages.size() - 1; i >= 0; i--) {
                Msg msg = messages.get(i);
                if (msg.getRole() == MsgRole.USER) {
                    String text = msg.getTextContent();
                    if (text != null && !text.isBlank()) return text;
                }
            }
            return "";
        }
    }
}
