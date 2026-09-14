package com.smartrecruit.aiengine.config;

import com.smartrecruit.aiengine.service.LlmGatewayService;
import com.smartrecruit.common.util.DateUtils;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.SystemMessage;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.harness.agent.HarnessAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * AgentScope2 Spring 配置。
 *
 * <p>当 {@code agentscope.enabled=true} 时激活，
 * 创建 HarnessAgent Bean 和将现有 LlmGatewayService 适配为 AgentScope2 Model 接口的适配器。</p>
 *
 * @since 2.0.0
 */
@Configuration
@ConditionalOnProperty(name = "agentscope.enabled", havingValue = "true")
@Slf4j
public class AgentScopeConfiguration {

    private final LlmGatewayService llmGatewayService;
    private final AgentScopeProperties agentScopeProperties;

    public AgentScopeConfiguration(LlmGatewayService llmGatewayService,
                                   AgentScopeProperties agentScopeProperties) {
        this.llmGatewayService = llmGatewayService;
        this.agentScopeProperties = agentScopeProperties;
    }

    /**
     * LangChain4j → AgentScope2 Model 接口适配器。
     *
     * <p>将现有的同步 LlmGatewayService 包装为 AgentScope2 的 {@link Model} 接口，
     * 使得 HarnessAgent 可以通过 AgentScope2 的编排框架调用已有的 LLM 能力。</p>
     */
    @Bean
    @Qualifier("llmGatewayModel")
    public Model llmGatewayModel() {
        return new Model() {

            @Override
            public String getModelName() {
                return "smartrecruit-llm-gateway";
            }

            @Override
            public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> toolSchemas,
                                              GenerateOptions options) {
                // 提取 system prompt 和 user prompt
                String systemPrompt = extractSystemPrompt(messages);
                String userPrompt = extractUserPrompt(messages);

                // AgentScope2 HarnessAgent 可能不通过 SystemMessage 传递 sysPrompt，
                // 抽取失败时回退到预置的面试评估 System Prompt
                if (systemPrompt == null || systemPrompt.isBlank()) {
                    systemPrompt = AiEvaluationPrompts.SYSTEM_PROMPT;
                }

                log.debug("[AgentScope2适配] systemPrompt长度={}, userPrompt长度={}",
                        systemPrompt != null ? systemPrompt.length() : 0,
                        userPrompt != null ? userPrompt.length() : 0);

                try {
                    // 编排场景模型可能返回工具调用/自由文本等非 JSON 内容，
                    // 必须取原始文本，不能走 chat() 的 JSON 解析
                    String textContent = llmGatewayService.chatRaw(systemPrompt, userPrompt);

                    ChatResponse response = ChatResponse.builder()
                            .id("as2-" + DateUtils.currentEpochMillis())
                            .content(List.of(TextBlock.builder().text(textContent).build()))
                            .finishReason("stop")
                            .build();

                    return Flux.just(response);
                } catch (Exception e) {
                    log.error("[AgentScope2适配] LLM调用失败", e);
                    return Flux.error(e);
                }
            }
        };
    }

    /**
     * Spring Boot 建议通过 {@link #llmGatewayModel()} 和
     * {@code HarnessAgent.builder().model(model).build()} 的模式在业务 Service 中按需创建 Agent。
     *
     * <p>这里提供一个便捷方法用于获取已配置的工作区路径，供业务类使用。</p>
     */
    @Bean
    @Qualifier("agentScopeWorkspacePath")
    public java.nio.file.Path workspacePath() {
        return Paths.get(agentScopeProperties.getWorkspace().getPath());
    }

    /**
     * 面试评估 HarnessAgent — 当 {@code agentscope.sub-agents.interview-evaluator.enabled=true} 时创建。
     */
    @Bean
    @Qualifier("harnessInterviewEvaluatorAgent")
    @ConditionalOnProperty(name = "agentscope.sub-agents.interview-evaluator.enabled", havingValue = "true")
    public HarnessAgent harnessInterviewEvaluatorAgent() {
        log.info("创建面试评估 HarnessAgent...");

        HarnessAgent agent = HarnessAgent.builder()
                .name("interview-evaluator")
                .description("AI面试评估智能体，对候选人面试表现进行六维度评估")
                .sysPrompt(AiEvaluationPrompts.SYSTEM_PROMPT)
                .model(llmGatewayModel())
                .workspace(workspacePath())
                .maxIters(3)
                .enableTaskList(false)
                .checkRunning(false)
                .build();

        log.info("面试评估 HarnessAgent 创建成功: agentId={}, model={}",
                agent.getAgentId(), agent.getModel().getModelName());
        return agent;
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private String extractSystemPrompt(List<Msg> messages) {
        if (messages == null) return "";
        for (Msg msg : messages) {
            if (msg instanceof SystemMessage || msg.getRole() == MsgRole.SYSTEM) {
                return msg.getTextContent();
            }
        }
        // 回退：检查 metadata 中是否有 systemPrompt
        for (Msg msg : messages) {
            if (msg.getMetadata() != null && msg.getMetadata().containsKey("systemPrompt")) {
                Object sp = msg.getMetadata().get("systemPrompt");
                return sp != null ? sp.toString() : "";
            }
        }
        return "";
    }

    private String extractUserPrompt(List<Msg> messages) {
        if (messages == null) return "";
        // 取最后一条 user 消息的文本
        for (int i = messages.size() - 1; i >= 0; i--) {
            Msg msg = messages.get(i);
            if (msg.getRole() == MsgRole.USER) {
                String text = msg.getTextContent();
                if (text != null && !text.isBlank()) {
                    // 如果 metadata 中还有 userPrompt，合并
                    if (msg.getMetadata() != null && msg.getMetadata().containsKey("userPrompt")) {
                        return msg.getMetadata().get("userPrompt").toString();
                    }
                    return text;
                }
            }
        }
        return "";
    }
}
