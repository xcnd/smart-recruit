package com.smartrecruit.aiengine.controller;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.JobMatchTarget;
import com.smartrecruit.aiengine.domain.JobRequirement;
import com.smartrecruit.aiengine.enums.AiEnums;
import com.smartrecruit.aiengine.dto.request.OfferPredictRequest;
import com.smartrecruit.aiengine.dto.request.RetentionPredictRequest;
import com.smartrecruit.aiengine.dto.response.JdVO;
import com.smartrecruit.aiengine.dto.response.InterviewQuestionVO;
import com.smartrecruit.aiengine.dto.response.ResumeImageParseVO;
import com.smartrecruit.aiengine.dto.response.ResumeParseVO;
import com.smartrecruit.aiengine.dto.response.ScreenResultVO;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.aiengine.dto.response.MatchResultVO;
import com.smartrecruit.aiengine.dto.response.TalentRecommendVO;
import com.smartrecruit.aiengine.dto.request.TalentRecommendRequest;
import com.smartrecruit.aiengine.dto.request.AnalyticsInsightRequest;
import com.smartrecruit.aiengine.dto.response.AiInsightVO;
import com.smartrecruit.aiengine.dto.response.RetentionPredictVO;
import com.smartrecruit.aiengine.service.AgentCapabilityService;
import com.smartrecruit.aiengine.service.AgentService;
import com.smartrecruit.aiengine.service.AgentTaskRecorder;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Agent 能力网关控制器。
 *
 * <p>向业务模块暴露统一的 AI 能力入口（JD 生成、面试题生成、简历解析、
 * 智能筛选、Offer 预测、内推匹配），每次调用记录真实任务与指标。</p>
 *
 * @since 2026-04-06
 */
@RestController
@RequestMapping("/api/v1/agent-capabilities")
@RequiredArgsConstructor
@Slf4j
public class AgentCapabilityController {

    private final AgentCapabilityService capabilityService;
    private final AgentTaskRecorder taskRecorder;
    private final AgentService agentService;
    private final ObjectMapper objectMapper;

    /** 异步出题任务存储：taskId → 任务。 */
    private final Map<String, QuestionTask> questionTasks = new ConcurrentHashMap<>();

    /** 异步出题执行器（虚拟线程，LLM 耗时不影响 HTTP 响应）。 */
    private static final Executor QUESTION_EXECUTOR =
            Executors.newVirtualThreadPerTaskExecutor();

    public record JdRequest(String jobTitle, String department, String experienceLevel) {}

    public record QuestionRequest(Integer interviewType, Integer round,
                                  String jobTitle, String candidateName) {}

    public record ParseRequest(String fileName, String contentText) {}

    public record ParseImageRequest(String fileName, List<String> base64Images) {}

    public record ScreenRequest(CandidateProfile candidate, JobRequirement requirement) {}

    public record MatchRequest(CandidateProfile candidate, List<JobMatchTarget> jobs) {}

    /**
     * 生成职位描述（JD）文案。
     *
     * @param request 职位标题、部门与经验要求
     * @return 生成的 JD 文本（JSON）
     */
    @PostMapping("/jd/generate")
    public ApiResponse<JdVO> generateJd(@RequestBody JdRequest request) {
        return execute("jd-generator", AiEnums.TaskType.JD_GEN.getCode(), request,
                () -> capabilityService.generateJd(
                        request.jobTitle(), request.department(), request.experienceLevel()));
    }

    /**
     * 生成面试题。
     *
     * @param request 面试类型、轮次、职位与候选人信息
     * @return 生成的面试题列表（JSON）
     */
    @PostMapping("/interview/questions")
    public ApiResponse<List<InterviewQuestionVO>> generateQuestions(@RequestBody QuestionRequest request) {
        return execute("interview-question", AiEnums.TaskType.QUESTION_GEN.getCode(), request,
                () -> capabilityService.generateInterviewQuestions(
                        request.interviewType(),
                        request.round() == null ? 1 : request.round(),
                        request.jobTitle(), request.candidateName()));
    }

    /**
     * 异步生成面试题：立即返回任务 ID，前端/业务侧轮询
     * {@code GET /interview/questions/async/{taskId}} 获取结果，
     * 避免 LLM 生成耗时过长导致 HTTP 调用超时。
     */
    @PostMapping("/interview/questions/async")
    public ApiResponse<Map<String, String>> generateQuestionsAsync(
            @RequestBody QuestionRequest request) {
        String taskId = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        QuestionTask task = new QuestionTask();
        questionTasks.put(taskId, task);
        QUESTION_EXECUTOR.execute(() -> {
            task.status = "PROCESSING";
            try {
                List<InterviewQuestionVO> questions = capabilityService.generateInterviewQuestions(
                        request.interviewType(),
                        request.round() == null ? 1 : request.round(),
                        request.jobTitle(), request.candidateName());
                task.questions = questions == null ? List.of() : questions;
                task.status = "COMPLETED";
                log.info("异步出题完成: taskId={}, questions={}", taskId, task.questions.size());
            } catch (Exception e) {
                task.status = "FAILED";
                task.message = e.getMessage() == null ? "出题失败" : e.getMessage();
                log.error("异步出题失败: taskId={}, error={}", taskId, e.getMessage(), e);
            } finally {
                scheduleTaskCleanup(taskId);
            }
        });
        return ApiResponse.success(Map.of("taskId", taskId));
    }

    /**
     * 查询异步出题任务状态与结果。
     */
    @GetMapping("/interview/questions/async/{taskId}")
    public ApiResponse<Map<String, Object>> questionTask(@PathVariable String taskId) {
        QuestionTask task = questionTasks.get(taskId);
        if (task == null) {
            return ApiResponse.error(40401, "出题任务不存在: taskId=" + taskId);
        }
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("taskId", taskId);
        data.put("status", task.status);
        data.put("message", task.message);
        data.put("questions", task.questions);
        return ApiResponse.success(data);
    }

    /** 任务完成后延迟清理（保留 3 分钟供轮询兜底）。 */
    private void scheduleTaskCleanup(String taskId) {
        Thread cleaner = new Thread(() -> {
            try {
                Thread.sleep(180_000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            questionTasks.remove(taskId);
        }, "question-task-cleaner");
        cleaner.setDaemon(true);
        cleaner.start();
    }

    /** 异步出题任务内部对象。 */
    private static class QuestionTask {
        private volatile String status = "PENDING";
        private volatile String message = "";
        private volatile List<InterviewQuestionVO> questions = List.of();
    }

    /**
     * 解析简历文本为结构化信息。
     *
     * @param request 文件名与简历文本内容
     * @return 结构化简历信息（JSON）
     */
    @PostMapping("/resume/parse")
    public ApiResponse<ResumeParseVO> parseResume(@RequestBody ParseRequest request) {
        return execute("resume-parser", AiEnums.TaskType.RESUME_PARSE.getCode(), request,
                () -> capabilityService.parseResume(request.fileName(), request.contentText()));
    }

    /**
     * 解析图片/扫描件简历（AI 视觉模型）。
     *
     * @param request 文件名与图片 base64 Data URL
     * @return 结构化简历信息；识别不完整时 reviewRequired=true
     */
    @PostMapping("/resume/parse-image")
    public ApiResponse<ResumeImageParseVO> parseResumeImage(@RequestBody ParseImageRequest request) {
        return execute("resume-parser", AiEnums.TaskType.RESUME_PARSE.getCode(), request,
                () -> capabilityService.parseResumeImage(request.fileName(), request.base64Images()));
    }

    /**
     * 智能筛选候选人。
     *
     * @param request 候选人画像与职位要求
     * @return 筛选评分与维度明细（JSON）
     */
    @PostMapping("/screen")
    public ApiResponse<ScreenResultVO> screen(@RequestBody ScreenRequest request) {
        return execute("smart-screener", AiEnums.TaskType.SCREEN.getCode(), request,
                () -> capabilityService.screenCandidate(request.candidate(), request.requirement()));
    }

    /**
     * 预测候选人接受 Offer 的概率与建议。
     *
     * @param request 候选人画像与 Offer 详情
     * @return 预测结果（JSON）
     */
    @PostMapping("/offer/predict")
    public ApiResponse<PredictOfferVO> predictOffer(@RequestBody OfferPredictRequest request) {
        return execute("offer-predictor", AiEnums.TaskType.PREDICT.getCode(), request,
                () -> capabilityService.predictOffer(request.candidate(), request.offer()));
    }

    /**
     * 预测新员工留存风险与留任概率。
     *
     * @param request 入职候选人画像
     * @return 6/12 个月留任概率、风险级别与干预建议
     */
    @PostMapping("/retention/predict")
    public ApiResponse<RetentionPredictVO> predictRetention(
            @RequestBody RetentionPredictRequest request) {
        return execute("retention-predictor", AiEnums.TaskType.PREDICT.getCode(), request,
                () -> capabilityService.predictRetention(request.candidate()));
    }

    /**
     * 内推职位智能匹配。
     *
     * @param request 候选人画像与目标职位列表
     * @return 匹配评分与推荐结果（JSON）
     */
    @PostMapping("/referral/match")
    public ApiResponse<MatchResultVO> matchReferral(@RequestBody MatchRequest request) {
        return execute("referral-matcher", AiEnums.TaskType.RECOMMEND.getCode(), request,
                () -> capabilityService.matchReferral(request.candidate(), request.jobs()));
    }

    /**
     * AI 人才推荐：为一批候选人匹配目标职位并排序。
     */
    @PostMapping("/talent/recommend")
    public ApiResponse<List<TalentRecommendVO>> recommendTalent(
            @RequestBody TalentRecommendRequest request) {
        return execute("talent-recommender", AiEnums.TaskType.RECOMMEND.getCode(), request,
                () -> capabilityService.recommendTalent(request.job(), request.candidates()));
    }

    /**
     * AI 数据分析洞察：基于统计数据生成自然语言洞察与建议。
     */
    @PostMapping("/analytics/insights")
    public ApiResponse<List<AiInsightVO>> analyticsInsights(
            @RequestBody AnalyticsInsightRequest request) {
        return execute("analytics-insights", AiEnums.TaskType.INSIGHTS.getCode(), request,
                () -> capabilityService.generateAnalyticsInsights(request));
    }

    private <T> ApiResponse<T> execute(String agent, int taskType, Object input,
                                       Supplier<T> supplier) {
        // 暂停/故障的智能体拒绝执行（业务侧均有降级兜底）
        if (!agentService.isAgentAvailable(agent)) {
            log.warn("Agent 当前不可用，能力调用被拒绝: agent={}", agent);
            throw new IllegalStateException("Agent 已暂停或未启用: " + agent);
        }
        long start = DateUtils.currentEpochMillis();
        // 真实任务生命周期：开始（执行中）→ 完成/失败（记录耗时与指标）
        long taskId = taskRecorder.startTask(agent, taskType, toJson(input));
        try {
            T result = supplier.get();
            taskRecorder.completeTask(taskId, true, toJson(result), elapsed(start), null);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("Agent 能力调用失败: agent={}, error={}", agent, e.getMessage(), e);
            taskRecorder.completeTask(taskId, false, "", elapsed(start), e.getMessage());
            throw e;
        }
    }

    private long elapsed(long start) {
        return DateUtils.currentEpochMillis() - start;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
