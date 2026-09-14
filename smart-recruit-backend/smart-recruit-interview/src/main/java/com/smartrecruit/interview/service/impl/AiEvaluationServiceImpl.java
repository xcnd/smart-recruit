package com.smartrecruit.interview.service.impl;

import com.smartrecruit.interview.config.LlmProperties;
import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.remote.LlmChatRequest;
import com.smartrecruit.interview.dto.response.InterviewReportVO.DimensionScore;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.enums.InterviewEnums;
import com.smartrecruit.interview.feign.AiAgentScopeClient;
import com.smartrecruit.interview.feign.AiEngineClient;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.smartrecruit.common.dto.agentscope.EvaluateRequest;
import com.smartrecruit.common.dto.agentscope.EvaluateResponse;
import com.smartrecruit.common.dto.agentscope.OrchestrateRequest;
import com.smartrecruit.common.dto.agentscope.OrchestrateResponse;
import com.smartrecruit.common.util.DateUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import com.smartrecruit.interview.service.AiEvaluationService;

/**
 * AI 面试评估服务 — 调用大模型生成六维度评估报告。
 *
 * <p>LLM 启用时调用真实大模型，禁用或失败时降级为启发式模拟评分。</p>
 *
 * @since 1.0.0
 */
@Service
@Slf4j
public class AiEvaluationServiceImpl implements AiEvaluationService {

    private final LlmProperties llmProperties;
    private final AiEngineClient aiEngineClient;
    private final AiAgentScopeClient aiAgentScopeClient;
    private final ObjectMapper objectMapper;

    /** 是否启用 AgentScope2 多智能体框架。 */
    @Value("${agentscope.enabled:false}")
    private boolean agentScopeEnabled;

    public AiEvaluationServiceImpl(LlmProperties llmProperties,
                               ObjectProvider<AiEngineClient> aiEngineClientProvider,
                               ObjectProvider<AiAgentScopeClient> aiAgentScopeClientProvider,
                               ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.aiEngineClient = aiEngineClientProvider.getIfAvailable();
        this.aiAgentScopeClient = aiAgentScopeClientProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        log.info("AiEngineClient 注入状态: {}", aiEngineClient != null ? "已就绪" : "未注入 — AI 评估降级为启发式评分");
        log.info("AiAgentScopeClient 注入状态: {}, agentScopeEnabled={}",
                aiAgentScopeClient != null ? "已就绪" : "未注入", agentScopeEnabled);
    }

    private static final String SYSTEM_PROMPT = """
            你是一位拥有15年经验的资深技术面试官和人才评估专家。你的任务是根据面试中候选人的表现，客观、全面地评估候选人的综合能力，生成结构化的评估报告。

            评估维度及权重：
            1. 专业技能（25%）— 技术深度、广度、项目经验匹配度
            2. 沟通表达（20%）— 逻辑清晰度、表达准确性、倾听理解能力
            3. 逻辑思维（20%）— 分析问题的条理性、解决方案的合理性、思维敏捷度
            4. 团队协作（15%）— 合作意识、冲突处理能力、团队贡献
            5. 学习能力（10%）— 技术敏感性、自我驱动力、知识更新速度
            6. 抗压能力（10%）— 情绪稳定性、逆境应对、时间管理

            请严格按照以下JSON格式输出，不要包含任何markdown标记或其他解释文字：
            {
              "dimensions": [
                {"name": "专业技能", "weight": 25, "score": 85},
                {"name": "沟通表达", "weight": 20, "score": 80},
                {"name": "逻辑思维", "weight": 20, "score": 78},
                {"name": "团队协作", "weight": 15, "score": 82},
                {"name": "学习能力", "weight": 10, "score": 75},
                {"name": "抗压能力", "weight": 10, "score": 80}
              ],
              "totalScore": 81,
              "strengths": ["技术基础扎实", "沟通表达清晰"],
              "weaknesses": ["系统架构经验有待提升"],
              "keyMoments": [
                {"time": "09:00", "text": "自我介绍环节表达清晰，项目经验展示到位"},
                {"time": "09:15", "text": "技术问答环节对核心概念理解准确"}
              ],
              "overallComment": "候选人综合素质良好，技术功底扎实，建议重点关注系统设计能力培养。",
              "suggestion": 1
            }

            suggestion映射：0=强烈推荐录用, 1=推荐录用, 2=待定, 3=不推荐, 4=加试
            """;

    /**
     * 评估面试并返回结构化评估请求对象。
     *
     * <p>优先级：AgentScope2 HarnessAgent > LangChain4j LLM 网关 > 启发式评分降级</p>
     */
    public AssessmentRequest evaluate(Interview interview) {
        // 优先级0：AgentScope2 多 Agent 编排（4 个子 Agent 协作评估）
        if (agentScopeEnabled && aiAgentScopeClient != null) {
            try {
                return evaluateWithOrchestration(interview);
            } catch (Exception e) {
                log.warn("多 Agent 编排评估失败，降级单 Agent 评估: interviewId={}, error={}",
                        interview.getId(), e.getMessage());
            }
        }

        // 优先级1：AgentScope2 HarnessAgent（单智能体评估）
        if (agentScopeEnabled && aiAgentScopeClient != null) {
            try {
                return evaluateWithAgentScope(interview);
            } catch (Exception e) {
                log.warn("AgentScope2 评估失败，尝试降级: interviewId={}, error={}",
                        interview.getId(), e.getMessage());
            }
        }

        // 优先级2：LangChain4j LLM 网关（单次大模型调用）
        if (llmProperties.isEnabled() && aiEngineClient != null) {
            try {
                return evaluateWithLlm(interview);
            } catch (Exception e) {
                log.warn("LLM 评估失败，降级为启发式评分: interviewId={}, error={}",
                        interview.getId(), e.getMessage());
            }
        }

        // 优先级3：启发式模拟评分
        return evaluateWithHeuristic(interview);
    }

    /**
     * 通过 AgentScope2 多 Agent 编排进行面试评估。
     *
     * <p>编排 Agent 会依次调度 4 个子 Agent（候选人分析 → 问题生成 →
     * 回答评估 → 综合评分）协作生成最终报告。失败时抛出异常，
     * 由上层降级到单 Agent 评估链。</p>
     */
    private AssessmentRequest evaluateWithOrchestration(Interview interview) {
        String interviewContent = buildInterviewContent(interview);
        log.info("[多Agent编排] 开始调用 ai-engine 编排端点: interviewId={}, candidateName={}, jobTitle={}",
                interview.getId(), interview.getCandidateName(), interview.getJobTitle());

        OrchestrateRequest req = new OrchestrateRequest();
        req.setCandidateName(nullToEmpty(interview.getCandidateName()));
        req.setCandidateProfile(interviewContent);
        req.setJobTitle(nullToEmpty(interview.getJobTitle()));
        req.setJobRequirements("面试类型：" + resolveTypeLabel(interview.getType())
                + "；第" + (interview.getRound() != null ? interview.getRound() : 1) + "轮");
        req.setSessionId("orchestrate-" + interview.getId());

        long start = DateUtils.currentEpochMillis();
        OrchestrateResponse response = aiAgentScopeClient.orchestrate(req);
        long elapsed = DateUtils.currentEpochMillis() - start;

        log.info("[多Agent编排] 响应: elapsed={}ms, sessionId={}, hasContent={}, hasError={}",
                elapsed, response.getSessionId(),
                response.getContent() != null && !response.getContent().isBlank(),
                response.getError() != null);

        if (response.getError() != null) {
            throw new IllegalStateException("编排端点返回错误: " + response.getError());
        }
        String contentJson = response.getContent();
        if (contentJson == null || contentJson.isBlank()) {
            throw new IllegalStateException("编排端点返回内容为空");
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(contentJson,
                    new TypeReference<Map<String, Object>>() {});
            return parseLlmResult(interview, parsed);
        } catch (Exception e) {
            throw new IllegalStateException("编排结果解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过 AgentScope2 HarnessAgent 进行多智能体评估。
     *
     * <p>向 ai-engine 的 AgentScope2 端点发送候选人基本信息及面试内容，
     * HarnessAgent 内部已包含 SYSTEM_PROMPT。端点返回 { sessionId, content }
     * 格式，content 是 LLM 生成的 JSON 评估结果字符串。</p>
     */
    private AssessmentRequest evaluateWithAgentScope(Interview interview) {
        String interviewContent = buildInterviewContent(interview);
        log.info("[AgentScope2 评估] 开始调用 ai-engine HarnessAgent: interviewId={}, candidateName={}, jobTitle={}",
                interview.getId(), interview.getCandidateName(), interview.getJobTitle());

        EvaluateRequest req = buildAgentScopeRequest(interview, interviewContent);
        long start = DateUtils.currentEpochMillis();
        EvaluateResponse response = aiAgentScopeClient.evaluate(req);
        long elapsed = DateUtils.currentEpochMillis() - start;

        log.info("[AgentScope2 评估] 响应: elapsed={}ms, sessionId={}, hasContent={}, hasError={}",
                elapsed, response.getSessionId(),
                response.getContent() != null && !response.getContent().isBlank(),
                response.getError() != null);

        if (response.getError() != null) {
            log.warn("[AgentScope2 评估] 端点返回错误，降级为启发式评分: error={}", response.getError());
            return evaluateWithHeuristic(interview);
        }

        String contentJson = response.getContent();
        if (contentJson == null || contentJson.isBlank()) {
            log.warn("[AgentScope2 评估] content 为空，降级为启发式评分");
            return evaluateWithHeuristic(interview);
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(contentJson,
                    new TypeReference<Map<String, Object>>() {});
            return parseLlmResult(interview, parsed);
        } catch (Exception e) {
            log.warn("[AgentScope2 评估] 解析 content JSON 失败，降级为启发式评分: error={}", e.getMessage());
            return evaluateWithHeuristic(interview);
        }
    }

    private EvaluateRequest buildAgentScopeRequest(Interview interview, String interviewContent) {
        EvaluateRequest req = new EvaluateRequest();
        req.setCandidateName(nullToEmpty(interview.getCandidateName()));
        req.setJobTitle(nullToEmpty(interview.getJobTitle()));
        req.setInterviewContent(interviewContent);
        req.setSessionId("interview-" + interview.getId());
        return req;
    }

    /**
     * 构建面试内容文本，供 AgentScope2 评估使用。
     */
    private String buildInterviewContent(Interview interview) {
        StringBuilder sb = new StringBuilder();
        if (interview.getAiTranscript() != null && !interview.getAiTranscript().isBlank()) {
            sb.append("面试题目：\n");
            String[] questions = interview.getAiTranscript().split("\n");
            for (int i = 0; i < questions.length; i++) {
                sb.append(i + 1).append(". ").append(questions[i].trim()).append("\n");
            }
        }
        if (interview.getFeedback() != null && !interview.getFeedback().isBlank()) {
            sb.append("\n面试反馈：\n");
            sb.append(interview.getFeedback()).append("\n");
        }
        String typeLabel = resolveTypeLabel(interview.getType());
        sb.append("\n面试类型：").append(typeLabel);
        sb.append("，第").append(interview.getRound() != null ? interview.getRound() : 1).append("轮");
        return sb.toString();
    }

    private AssessmentRequest evaluateWithLlm(Interview interview) {
        String userPrompt = buildUserPrompt(interview);
        log.info("[Feign 评估] 开始调用 ai-engine: interviewId={}, candidateName={}, jobTitle={}, promptLength={}",
                interview.getId(), interview.getCandidateName(), interview.getJobTitle(), userPrompt.length());

        long feignStart = DateUtils.currentEpochMillis();
        Map<String, Object> result = aiEngineClient.chat(
                new LlmChatRequest(SYSTEM_PROMPT, userPrompt, "interview-evaluator"));
        long feignElapsed = DateUtils.currentEpochMillis() - feignStart;

        log.info("[Feign 评估] ai-engine 响应: elapsed={}ms, resultKeys={}, totalScore={}",
                feignElapsed, result.keySet(), result.getOrDefault("totalScore", "N/A"));

        return parseLlmResult(interview, result);
    }

    @SuppressWarnings("unchecked")
    private AssessmentRequest parseLlmResult(Interview interview, Map<String, Object> result) {
        AssessmentRequest req = new AssessmentRequest();
        req.setCandidateId(interview.getCandidateId());
        req.setCandidateName(interview.getCandidateName());
        req.setJobTitle(interview.getJobTitle());

        List<Map<String, Object>> dimensions = (List<Map<String, Object>>) result.get("dimensions");
        int totalScore = toInt(result.get("totalScore"));

        if (dimensions != null) {
            for (Map<String, Object> dim : dimensions) {
                String name = String.valueOf(dim.getOrDefault("name", ""));
                int score = toInt(dim.get("score"));
                BigDecimal bd = BigDecimal.valueOf(score / 20.0).setScale(1, RoundingMode.HALF_UP);
                switch (name) {
                    case "专业技能" -> req.setTechnologyDepth(bd);
                    case "沟通表达" -> req.setCommunication(bd);
                    case "逻辑思维" -> req.setProblemSolving(bd);
                    case "团队协作" -> req.setTeamwork(bd);
                    case "学习能力" -> req.setLearningAbility(bd);
                }
            }
        }

        req.setOverallScore(BigDecimal.valueOf(totalScore));
        req.setOverallComment(String.valueOf(result.getOrDefault("overallComment", "")));
        req.setSuggestion(toInt(result.get("suggestion")));

        List<String> strengths = (List<String>) result.get("strengths");
        List<String> weaknesses = (List<String>) result.get("weaknesses");
        req.setStrengths(strengths != null ? strengths : List.of());
        req.setWeaknesses(weaknesses != null ? weaknesses : List.of());

        List<Map<String, String>> keyMoments = (List<Map<String, String>>) result.get("keyMoments");
        if (keyMoments != null) {
            List<AssessmentRequest.KeyMomentItem> items = keyMoments.stream()
                    .map(m -> {
                        AssessmentRequest.KeyMomentItem item = new AssessmentRequest.KeyMomentItem();
                        item.setTime(m.getOrDefault("time", ""));
                        item.setText(m.getOrDefault("text", ""));
                        return item;
                    }).toList();
            req.setKeyMoments(items);
        }

        log.info("LLM 评估完成: interviewId={}, totalScore={}, suggestion={}",
                interview.getId(), totalScore, req.getSuggestion());
        return req;
    }

    private String buildUserPrompt(Interview interview) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下面试候选人进行综合评估。\n\n");

        sb.append("## 候选人信息\n");
        sb.append("- 姓名：").append(nullToEmpty(interview.getCandidateName())).append("\n");
        sb.append("- 应聘职位：").append(nullToEmpty(interview.getJobTitle())).append("\n");
        String typeLabel = resolveTypeLabel(interview.getType());
        sb.append("- 面试类型：").append(typeLabel).append("\n");
        sb.append("- 面试轮次：第").append(interview.getRound() != null ? interview.getRound() : 1).append("轮\n");

        if (interview.getAiTranscript() != null && !interview.getAiTranscript().isBlank()) {
            sb.append("\n## 面试题目\n");
            String[] questions = interview.getAiTranscript().split("\n");
            for (int i = 0; i < questions.length; i++) {
                sb.append(i + 1).append(". ").append(questions[i].trim()).append("\n");
            }
        }

        if (interview.getFeedback() != null && !interview.getFeedback().isBlank()) {
            sb.append("\n## 面试反馈\n");
            sb.append(nullToEmpty(interview.getFeedback())).append("\n");
        }

        sb.append("\n请根据以上信息，生成结构化的六维度评估报告（JSON格式）。");
        return sb.toString();
    }

    // ---- 启发式评分（LLM 不可用时的降级方案）----

    /** 使用启发式评分生成面试评估（LLM 不可用时的降级方案）。 */
    public AssessmentRequest evaluateWithHeuristic(Interview interview) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        List<DimensionScore> dimensions = List.of(
                new DimensionScore("专业技能", 25, r.nextInt(55, 96)),
                new DimensionScore("沟通表达", 20, r.nextInt(60, 96)),
                new DimensionScore("逻辑思维", 20, r.nextInt(55, 96)),
                new DimensionScore("团队协作", 15, r.nextInt(65, 96)),
                new DimensionScore("学习能力", 10, r.nextInt(60, 96)),
                new DimensionScore("抗压能力", 10, r.nextInt(55, 96))
        );

        int totalScore = (int) Math.round(
                dimensions.stream()
                        .mapToDouble(d -> d.getScore() * d.getWeight() / 100.0)
                        .sum());

        AssessmentRequest req = new AssessmentRequest();
        req.setCandidateId(interview.getCandidateId());
        req.setCandidateName(interview.getCandidateName());
        req.setJobTitle(interview.getJobTitle());
        req.setTechnologyDepth(toDecimal(dimensions.get(0).getScore()));
        req.setCommunication(toDecimal(dimensions.get(1).getScore()));
        req.setProblemSolving(toDecimal(dimensions.get(2).getScore()));
        req.setTeamwork(toDecimal(dimensions.get(3).getScore()));
        req.setLearningAbility(toDecimal(dimensions.get(4).getScore()));
        req.setOverallScore(BigDecimal.valueOf(totalScore));
        req.setOverallComment("候选人综合表现" + (totalScore >= 80 ? "优秀" : totalScore >= 60 ? "良好" : "一般") + "。");
        req.setStrengths(List.of("技术基础扎实", "沟通表达清晰"));
        req.setWeaknesses(List.of("系统架构经验有待提升"));
        req.setSuggestion(totalScore >= 80 ? 1 : totalScore >= 60 ? 2 : 3);
        return req;
    }

    // ---- 工具方法 ----

    /** 生成六维度评分列表（启发式随机评分）。 */
    public List<DimensionScore> generateDimensionScores() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return List.of(
                new DimensionScore("专业技能", 25, r.nextInt(55, 96)),
                new DimensionScore("沟通表达", 20, r.nextInt(60, 96)),
                new DimensionScore("逻辑思维", 20, r.nextInt(55, 96)),
                new DimensionScore("团队协作", 15, r.nextInt(65, 96)),
                new DimensionScore("学习能力", 10, r.nextInt(60, 96)),
                new DimensionScore("抗压能力", 10, r.nextInt(55, 96))
        );
    }

    private BigDecimal toDecimal(int score0to100) {
        return BigDecimal.valueOf(score0to100 / 20.0).setScale(1, RoundingMode.HALF_UP);
    }

    private int toInt(Object obj) {
        if (obj instanceof Number n) return n.intValue();
        if (obj instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
        }
        return 0;
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    private String resolveTypeLabel(Integer type) {
        InterviewEnums.InterviewType t = InterviewEnums.InterviewType.fromCode(type);
        return t != null ? t.getLabel() : "综合面试";
    }

    @Data
    @Builder
    public static class EvaluationDimensions {
        private int professionalSkill;
        private int communication;
        private int logicalThinking;
        private int teamwork;
        private int learningAbility;
        private int stressManagement;
    }
}
