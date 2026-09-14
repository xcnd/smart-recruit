package com.smartrecruit.aiengine.pipeline;

import com.smartrecruit.aiengine.agents.v2.InterviewEvaluatorAgentV2;
import com.smartrecruit.aiengine.config.AiEvaluationPrompts;
import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.InterviewReport;
import com.smartrecruit.aiengine.domain.InterviewSession;
import com.smartrecruit.aiengine.domain.JobRequirement;
import com.smartrecruit.aiengine.domain.OfferDetail;
import com.smartrecruit.aiengine.dto.request.RunPipelineRequest;
import com.smartrecruit.aiengine.dto.response.PipelineResultVO;
import com.smartrecruit.aiengine.dto.response.PipelineStageVO;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.aiengine.dto.response.ScreenResultVO;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.service.AgentCapabilityService;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.aiengine.service.LlmGatewayService;
import com.smartrecruit.common.util.DateUtils;
import io.agentscope.core.message.Msg;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历到 Offer 全流程编排器。
 *
 * <p>把「简历解析 → 智能筛选 → 人工审核 → 面试评估 → Offer 预测」串成一条
 * 真实可运行的编排流水线：每个阶段调用真实能力链路（LLM 优先、启发式兜底），
 * 并通过 {@link AgentTaskRecorder} 落库任务与事件，运行结果会实时出现在
 * 「AI 智能编排」监控页的任务队列与事件流中。</p>
 *
 * <p>编排入口：POST /api/v1/agents/pipeline/run</p>
 *
 * @since 2026-04-09
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ResumeToOfferPipeline {

    private static final String PIPELINE_NAME = "resume-to-offer";

    /** 常用技能词典，用于从简历文本中提取技能。 */
    private static final List<String> SKILL_DICTIONARY = List.of(
            "Java", "Python", "Go", "Rust", "TypeScript", "JavaScript", "React", "Vue",
            "Spring", "Spring Boot", "Spring Cloud", "MyBatis", "MySQL", "Redis", "Kafka",
            "RabbitMQ", "Elasticsearch", "Docker", "Kubernetes", "K8s", "Linux", "Nginx",
            "TensorFlow", "PyTorch", "Spark", "Flink", "Hadoop", "AWS", "GCP", "Azure",
            "CI/CD", "Jenkins", "Git", "Terraform", "微服务", "分布式", "高并发", "性能优化");

    private static final Pattern YEARS_PATTERN = Pattern.compile(
            "(\\d+)\\s*(?:年|年以上|年经验|years?|yrs?)");

    private final AgentCapabilityService capabilityService;
    private final AgentTaskRecorder taskRecorder;
    private final ObjectProvider<InterviewEvaluatorAgentV2> evaluatorV2Provider;
    private final ObjectProvider<LlmGatewayService> llmGatewayProvider;
    private final ObjectMapper objectMapper;

    /**
     * 执行从简历到 Offer 的完整编排流水线。
     *
     * @param request 候选人、简历文本、职位要求与 Offer 信息
     * @return 各阶段执行明细与关键输出
     */
    public PipelineResultVO execute(RunPipelineRequest request) {
        long start = DateUtils.currentEpochMillis();
        List<PipelineStageVO> stages = new ArrayList<>();
        boolean pipelineSuccess = true;

        // ---------- 阶段一：简历解析 ----------
        long stageStart = DateUtils.currentEpochMillis();
        CandidateProfile candidate = parseCandidate(request);
        long parseStageMs = DateUtils.currentEpochMillis() - stageStart;
        taskRecorder.record("resume-parser", AiEnums.TaskType.RESUME_PARSE.getCode(),
                toJson(request), toJson(candidate), true, parseStageMs, null);
        stages.add(stage("resume-parse", "简历解析", "resume-parser", "简历解析",
                "COMPLETED", parseStageMs,
                String.format("提取 %d 项技能，%s，%s 学历",
                        candidate.getSkills().size(),
                        candidate.getYearsOfExperience() + " 年经验",
                        candidate.getEducationLevel())));

        // ---------- 阶段二：智能筛选 ----------
        ScreenResultVO screening = null;
        try {
            long screenStart = DateUtils.currentEpochMillis();
            JobRequirement requirement = JobRequirement.builder()
                    .jobTitle(request.jobTitle())
                    .requiredSkills(request.requiredSkills() == null ? List.of() : request.requiredSkills())
                    .minYearsOfExperience(request.minYearsOfExperience())
                    .educationLevel("BACHELOR")
                    .build();
            screening = capabilityService.screenCandidate(candidate, requirement);
            long screenStageMs = DateUtils.currentEpochMillis() - screenStart;
            taskRecorder.record("smart-screener", AiEnums.TaskType.SCREEN.getCode(),
                    toJson(requirement), toJson(screening), true, screenStageMs, null);
            String screeningMsg = String.format("综合评分 %.1f 分，%s，建议：%s",
                    screening.overallScore(),
                    Boolean.TRUE.equals(screening.passed()) ? "通过筛选" : "未通过筛选",
                    screening.recommendation());
            stages.add(stage("smart-screen", "简历筛选", "smart-screener", "简历筛选",
                    "COMPLETED", screenStageMs, screeningMsg));
        } catch (Exception e) {
            pipelineSuccess = false;
            taskRecorder.record("smart-screener", AiEnums.TaskType.SCREEN.getCode(),
                    toJson(request), "", false, 0, e.getMessage());
            stages.add(stage("smart-screen", "简历筛选", "smart-screener", "简历筛选",
                    "FAILED", null,
                    "筛选失败：" + e.getMessage()));
        }

        // ---------- 阶段三：人工审核（编排中标记为人工环节，跳过） ----------
        stages.add(stage("human-review", "人工审核", "human-review", "人工审核",
                "SKIPPED", null, "人工审核环节，由 HR 在候选人中心线下完成"));

        // ---------- 阶段四：面试评估（仅当提供面试摘要时执行） ----------
        InterviewReport interviewReport = null;
        if (request.interviewSummary() != null && !request.interviewSummary().isBlank()) {
            try {
                long interviewStart = DateUtils.currentEpochMillis();
                InterviewSession session = InterviewSession.builder()
                        .sessionId(DateUtils.currentEpochMillis())
                        .candidateId(1L)
                        .jobId(1L)
                        .interviewType("TECHNICAL")
                        .round(1)
                        .transcript(request.interviewSummary())
                        .durationMinutes(45)
                        .build();
                // 真实 AI 评估：AgentScope2 HarnessAgent 优先，LLM 网关兜底
                interviewReport = evaluateWithAi(
                        request.candidateName(), request.jobTitle(),
                        request.interviewSummary(), session.getSessionId());
                long interviewStageMs = DateUtils.currentEpochMillis() - interviewStart;
                taskRecorder.record("interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                        toJson(session), toJson(interviewReport), true, interviewStageMs, null);
                stages.add(stage("interview-evaluate", "面试评估", "interview-evaluator", "面试评估",
                        "COMPLETED", interviewStageMs,
                        String.format("综合评分 %.1f 分，建议：%s",
                                interviewReport.getOverallScore(),
                                interviewReport.getHireRecommendation())));
            } catch (Exception e) {
                pipelineSuccess = false;
                taskRecorder.record("interview-evaluator", AiEnums.TaskType.EVALUATE.getCode(),
                        request.interviewSummary(), "", false, 0, e.getMessage());
                stages.add(stage("interview-evaluate", "面试评估", "interview-evaluator", "面试评估",
                        "FAILED", null,
                        "面试评估失败：" + e.getMessage()));
            }
        } else {
            stages.add(stage("interview-evaluate", "面试评估", "interview-evaluator", "面试评估",
                    "SKIPPED", null, "未提供面试摘要，跳过面试评估阶段"));
        }

        // ---------- 阶段五：Offer 接受度预测 ----------
        PredictOfferVO offerPrediction = null;
        try {
            long predictStart = DateUtils.currentEpochMillis();
            OfferDetail offer = OfferDetail.builder()
                    .jobTitle(request.jobTitle())
                    .totalPackage(request.offerTotalPackage() == null ? 0.0 : request.offerTotalPackage())
                    .build();
            offerPrediction = capabilityService.predictOffer(candidate, offer);
            long predictStageMs = DateUtils.currentEpochMillis() - predictStart;
            taskRecorder.record("offer-predictor", AiEnums.TaskType.PREDICT.getCode(),
                    toJson(offer), toJson(offerPrediction), true, predictStageMs, null);
            stages.add(stage("offer-predict", "Offer 预测", "offer-predictor", "Offer 预测",
                    "COMPLETED", predictStageMs,
                    String.format("接受概率 %.1f%%，风险等级 %s，建议：%s",
                            offerPrediction.acceptanceProbability(),
                            offerPrediction.riskLevel(),
                            offerPrediction.recommendation())));
        } catch (Exception e) {
            pipelineSuccess = false;
            taskRecorder.record("offer-predictor", AiEnums.TaskType.PREDICT.getCode(),
                    toJson(request), "", false, 0, e.getMessage());
            stages.add(stage("offer-predict", "Offer 预测", "offer-predictor", "Offer 预测",
                    "FAILED", null,
                    "Offer 预测失败：" + e.getMessage()));
        }

        long totalDurationMs = DateUtils.currentEpochMillis() - start;
        String overallStatus = pipelineSuccess ? "COMPLETED" : "FAILED";

        // 编排器汇总任务（orchestrator 真实活动，供监控页展示）
        taskRecorder.record("orchestrator", AiEnums.TaskType.ORCHESTRATE.getCode(),
                String.format("候选人 %s / 职位 %s", request.candidateName(), request.jobTitle()),
                String.format("%d 个阶段执行完成，总耗时 %dms", stages.size(), totalDurationMs),
                pipelineSuccess, totalDurationMs, pipelineSuccess ? null : "部分阶段执行失败");

        log.info("全流程编排完成: candidate={}, job={}, status={}, durationMs={}",
                request.candidateName(), request.jobTitle(), overallStatus, totalDurationMs);

        return new PipelineResultVO(PIPELINE_NAME, overallStatus, totalDurationMs,
                candidate, screening, interviewReport, offerPrediction, stages);
    }

    // ================================================================
    // 私有辅助方法
    // ================================================================

    /** 从请求与简历文本中构建候选人画像（真实文本解析，无随机数据）。 */
    private CandidateProfile parseCandidate(RunPipelineRequest request) {
        String text = request.resumeText() == null ? "" : request.resumeText();
        String lower = text.toLowerCase(Locale.ROOT);

        // 技能提取：简历文本中命中技能词典 + 职位必备技能
        Set<String> skills = new LinkedHashSet<>();
        for (String skill : SKILL_DICTIONARY) {
            if (lower.contains(skill.toLowerCase(Locale.ROOT))) {
                skills.add(skill);
            }
        }
        if (request.requiredSkills() != null) {
            for (String skill : request.requiredSkills()) {
                if (skill != null && !skill.isBlank()) {
                    skills.add(skill.trim());
                }
            }
        }

        // 年限提取
        int years = request.minYearsOfExperience() != null
                ? Math.max(request.minYearsOfExperience(), 1) : 1;
        Matcher matcher = YEARS_PATTERN.matcher(text);
        if (matcher.find()) {
            years = Integer.parseInt(matcher.group(1));
        }

        // 学历提取
        String education = "BACHELOR";
        if (lower.contains("博士") || lower.contains("phd")) {
            education = "PHD";
        } else if (lower.contains("硕士") || lower.contains("研究生") || lower.contains("master")) {
            education = "MASTER";
        } else if (lower.contains("大专") || lower.contains("associate")) {
            education = "ASSOCIATE";
        }

        CandidateProfile profile = CandidateProfile.builder()
                .name(request.candidateName())
                .email(request.candidateName() + "@example.com")
                .phone("")
                .educationLevel(education)
                .school("")
                .major("")
                .yearsOfExperience(years)
                .currentCompany("")
                .currentPosition("")
                .skills(new ArrayList<>(skills))
                .summary("简历文本解析结果：" + (text.length() > 200 ? text.substring(0, 200) + "..." : text))
                .build();
        return profile;
    }

    /**
     * 真实 AI 面试评估：AgentScope2 HarnessAgent 优先，LLM 网关兜底。
     *
     * <p>两者都走 interview-evaluator 路由模型（如 deepseek-v4），
     * 并计入 Token 消耗与 Agent 任务/事件。</p>
     */
    private InterviewReport evaluateWithAi(String candidateName, String jobTitle,
                                           String transcript, Long sessionId) {
        // 1) AgentScope2 HarnessAgent（多步推理评估）
        InterviewEvaluatorAgentV2 v2 = evaluatorV2Provider.getIfAvailable();
        if (v2 != null) {
            try {
                Msg msg = v2.evaluate(candidateName, jobTitle, transcript, "pipeline-" + sessionId)
                        .block(Duration.ofSeconds(90));
                if (msg != null && msg.getTextContent() != null && !msg.getTextContent().isBlank()) {
                    Map<String, Object> data = objectMapper.readValue(
                            msg.getTextContent(), new TypeReference<Map<String, Object>>() {});
                    InterviewReport report = toInterviewReport(data, sessionId);
                    if (report != null && report.getOverallScore() != null) {
                        log.info("AgentScope2 面试评估完成: sessionId={}, overallScore={}",
                                sessionId, report.getOverallScore());
                        return report;
                    }
                }
            } catch (Exception e) {
                log.warn("AgentScope2 面试评估失败，回退 LLM 网关: sessionId={}, error={}",
                        sessionId, e.getMessage());
            }
        }

        // 2) LLM 网关（单次大模型评估）
        LlmGatewayService llm = llmGatewayProvider.getIfAvailable();
        if (llm != null) {
            String userPrompt = buildEvaluationPrompt(candidateName, jobTitle, transcript);
            Map<String, Object> data = llm.chat(
                    AiEvaluationPrompts.SYSTEM_PROMPT, userPrompt, "interview-evaluator");
            InterviewReport report = toInterviewReport(data, sessionId);
            if (report != null && report.getOverallScore() != null) {
                log.info("LLM 网关面试评估完成: sessionId={}, overallScore={}",
                        sessionId, report.getOverallScore());
                return report;
            }
        }

        throw new IllegalStateException("面试评估 AI 链路不可用（AgentScope 与 LLM 网关均失败或返回为空）");
    }

    /** 组装面试评估用户提示词。 */
    private String buildEvaluationPrompt(String candidateName, String jobTitle, String transcript) {
        return String.format("候选人：%s\n应聘职位：%s\n\n## 面试内容\n%s",
                candidateName == null ? "未知" : candidateName,
                jobTitle == null ? "未知" : jobTitle,
                transcript == null ? "" : transcript);
    }

    /** 将 AI 评估 JSON 映射为评估报告。 */
    @SuppressWarnings("unchecked")
    private InterviewReport toInterviewReport(Map<String, Object> data, Long sessionId) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        Map<String, Double> dimensionScores = new LinkedHashMap<>();
        if (data.get("dimensions") instanceof List<?> dimensions) {
            for (Object item : dimensions) {
                if (item instanceof Map<?, ?> m) {
                    Object name = m.get("name");
                    Object score = m.get("score");
                    if (name != null && score instanceof Number n) {
                        dimensionScores.put(String.valueOf(name), n.doubleValue());
                    }
                }
            }
        }
        return InterviewReport.builder()
                .sessionId(sessionId)
                .overallScore(toDoubleOrNull(data.get("overallScore")))
                .dimensionScores(dimensionScores.isEmpty() ? null : dimensionScores)
                .communicationAssessment(toStr(data.get("overallComment")))
                .strengths(listToText(data.get("strengths")))
                .weaknesses(listToText(data.get("weaknesses")))
                .hireRecommendation(mapSuggestion(data.get("suggestion")))
                .confidenceLevel(85.0)
                .build();
    }

    /** 建议值映射：ADVANCE→HIRE、RETEST→CONSIDER、REJECT→REJECT。 */
    private String mapSuggestion(Object suggestion) {
        if (suggestion == null) {
            return null;
        }
        return switch (String.valueOf(suggestion).trim().toUpperCase(Locale.ROOT)) {
            case "ADVANCE" -> "HIRE";
            case "RETEST" -> "CONSIDER";
            case "REJECT" -> "REJECT";
            default -> String.valueOf(suggestion).trim();
        };
    }

    /** 将数组字段拼接为文本。 */
    private String listToText(Object value) {
        if (!(value instanceof List<?> list)) {
            return null;
        }
        return String.join("；", list.stream().map(String::valueOf).toList());
    }

    private Double toDoubleOrNull(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String toStr(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /** 组装阶段 VO。 */
    private PipelineStageVO stage(String stage, String stageName, String agentId,
                                  String agentName, String status, Long durationMs, String message) {
        return new PipelineStageVO(stage, stageName, agentId, agentName, status, durationMs, message);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
