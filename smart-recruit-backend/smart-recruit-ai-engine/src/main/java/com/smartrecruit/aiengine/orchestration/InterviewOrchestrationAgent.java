package com.smartrecruit.aiengine.orchestration;

import com.smartrecruit.aiengine.config.AgentScopeProperties;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 面试编排 Agent — 使用 AgentScope2 子 Agent 编排实现面试全流程。
 *
 * <p>编排流程：
 * <ol>
 *   <li>候选人分析子 Agent — 解析候选人背景，提取关键信息</li>
 *   <li>问题生成子 Agent — 根据候选人和职位生成个性化面试问题</li>
 *   <li>回答评估子 Agent — 评估面试回答的质量</li>
 *   <li>综合评分子 Agent — 汇总各维度评分，生成最终报告</li>
 * </ol>
 *
 * <p>子 Agent 通过 {@link SubagentDeclaration} 声明，
 * 由 HarnessAgent 的 {@code agent_spawn} 机制异步调度。</p>
 *
 * @since 2.0.0
 */
@Component
@ConditionalOnProperty(name = "agentscope.sub-agents.interview-orchestrator.enabled", havingValue = "true")
@Slf4j
public class InterviewOrchestrationAgent {

    private final Model model;
    private final AgentScopeProperties properties;
    /** 当前生效的编排 HarnessAgent 实例（重启时整体重建并替换）。 */
    private final AtomicReference<HarnessAgent> orchestratorRef = new AtomicReference<>();

    public InterviewOrchestrationAgent(Model model,
                                        AgentScopeProperties properties) {
        this.model = model;
        this.properties = properties;
        this.orchestratorRef.set(buildAgent());
        log.info("InterviewOrchestrationAgent 初始化完成: agentId={}, subAgentCount={}",
                orchestratorRef.get().getAgentId(), 4);
    }

    /**
     * 重建编排 HarnessAgent 实例（重启时真正重新加载运行时、子 Agent 与配置）。
     */
    public HarnessAgent rebuild() {
        HarnessAgent fresh = buildAgent();
        orchestratorRef.set(fresh);
        log.info("InterviewOrchestrationAgent 已重建: agentId={}",
                fresh.getAgentId());
        return fresh;
    }

    private HarnessAgent buildAgent() {

        // 定义子 Agent
        List<SubagentDeclaration> subAgents = List.of(
                SubagentDeclaration.builder()
                        .name("candidate-analyzer")
                        .description("分析候选人背景信息，提取关键能力维度")
                        .inlineAgentsBody("""
                                你是一位专业的候选人分析专家。请根据候选人信息，
                                从技术能力、项目经验、教育背景、职业发展四个维度进行分析，
                                输出结构化的候选人画像。""")
                        .build(),
                SubagentDeclaration.builder()
                        .name("question-generator")
                        .description("根据职位要求和候选人背景生成针对性面试问题")
                        .inlineAgentsBody("""
                                你是一位资深的面试官。请根据职位要求和候选人背景，
                                生成5-8个针对性面试问题，覆盖技术深度、问题解决和沟通表达三个维度。
                                每个问题附带评分要点。""")
                        .build(),
                SubagentDeclaration.builder()
                        .name("answer-evaluator")
                        .description("评估候选人的面试回答质量")
                        .inlineAgentsBody("""
                                你是一位面试评估专家。请评估候选人对每个面试问题的回答质量，
                                从准确性、深度、逻辑性三个维度打分（1-5分），并给出评语。""")
                        .build(),
                SubagentDeclaration.builder()
                        .name("final-scorer")
                        .description("汇总各维度评分，生成最终面试报告")
                        .inlineAgentsBody("""
                                你是一位HR面试总结专家。请汇总所有子维度的评估结果，
                                生成最终的六维度面试报告。请严格按照以下 JSON 格式输出，不要包含任何 markdown 标记：
                                {
                                  "dimensions": [{"name":"专业技能","weight":25,"score":85},
                                    {"name":"沟通表达","weight":20,"score":80},
                                    {"name":"逻辑思维","weight":20,"score":78},
                                    {"name":"团队协作","weight":15,"score":82},
                                    {"name":"学习能力","weight":10,"score":75},
                                    {"name":"抗压能力","weight":10,"score":80}],
                                  "totalScore": 81,
                                  "strengths": ["优势1","优势2"],
                                  "weaknesses": ["待改进1"],
                                  "keyMoments": [{"time":"09:00","text":"关键时刻描述"}],
                                  "overallComment": "综合评语",
                                  "suggestion": 1
                                }
                                suggestion 取值：0=强烈推荐录用,1=推荐录用,2=待定,3=不推荐,4=加试。""")
                        .build()
        );

        return HarnessAgent.builder()
                .name("interview-orchestrator")
                .description("面试全流程编排智能体，协调多个子Agent完成面试评估")
                .sysPrompt("""
                        你是面试流程的总协调人。你的职责是：
                        1. 接收候选人信息和面试内容
                        2. 调度子Agent按流程执行评估
                        3. 汇总各阶段结果，生成最终面试报告

                        请使用 agent_spawn 依次调用子Agent完成评估流程。""")
                .model(model)
                .workspace(Paths.get(properties.getWorkspace().getPath()))
                .subagents(subAgents)
                .maxIters(5)
                .enableTaskList(false)
                .checkRunning(false)
                .build();
    }

    /**
     * 执行完整的面试编排流程。
     *
     * @param candidateName    候选人姓名
     * @param candidateProfile 候选人背景信息
     * @param jobTitle         应聘职位
     * @param jobRequirements  职位要求
     * @param sessionId        会话标识
     * @return 最终的面试评估报告
     */
    public Mono<Msg> orchestrate(String candidateName, String candidateProfile,
                                  String jobTitle, String jobRequirements,
                                  String sessionId) {
        String prompt = buildOrchestrationPrompt(candidateName, candidateProfile,
                jobTitle, jobRequirements);

        RuntimeContext ctx = RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("system")
                .build();

        UserMessage userMsg = new UserMessage(prompt);

        log.info("面试编排开始: candidateName={}, sessionId={}", candidateName, sessionId);
        return orchestratorRef.get().call(userMsg, ctx)
                .doOnSuccess(result -> log.info("面试编排完成: sessionId={}", sessionId))
                .doOnError(e -> log.error("面试编排失败: sessionId={}", sessionId, e));
    }

    public HarnessAgent getOrchestrator() {
        return orchestratorRef.get();
    }

    // ================================================================
    // 私有方法
    // ================================================================

    private String buildOrchestrationPrompt(String candidateName, String candidateProfile,
                                             String jobTitle, String jobRequirements) {
        return """
                ## 面试评估任务

                请按以下流程完成对候选人的完整面试评估：

                ### 候选人信息
                - 姓名：%s
                - 背景：%s

                ### 职位信息
                - 职位：%s
                - 要求：%s

                ### 评估流程
                1. 先用 candidate-analyzer 分析候选人背景
                2. 再用 question-generator 生成针对性问题
                3. 如果你的输入中包含面试回答，用 answer-evaluator 评估
                4. 最后用 final-scorer 生成综合报告

                请开始执行评估流程。""".formatted(
                candidateName != null ? candidateName : "未知",
                candidateProfile != null ? candidateProfile : "无",
                jobTitle != null ? jobTitle : "未知",
                jobRequirements != null ? jobRequirements : "无");
    }
}
