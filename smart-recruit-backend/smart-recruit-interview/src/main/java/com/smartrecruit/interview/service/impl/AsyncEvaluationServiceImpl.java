package com.smartrecruit.interview.service.impl;

import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.response.InterviewReportVO.DimensionScore;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.repository.InterviewMapper;
import com.smartrecruit.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.smartrecruit.interview.service.AsyncEvaluationService;
import com.smartrecruit.interview.service.AiEvaluationService;
import com.smartrecruit.interview.service.AssessmentService;

/**
 * 异步 AI 评估服务 — 通过虚拟线程将 AI 评估从同步 HTTP 请求中解耦。
 *
 * <p>使用 {@link Thread#startVirtualThread(Runnable)} 启动虚拟线程，
 * 不占用平台线程池，不依赖 Spring {@code @Async} AOP 代理。</p>
 *
 * @since 2.0.0
 */
@Service
@Slf4j
public class AsyncEvaluationServiceImpl implements AsyncEvaluationService {

    private final InterviewMapper interviewMapper;
    private final AiEvaluationService aiEvaluationService;
    private final AssessmentService assessmentService;
    private final ObjectMapper objectMapper;

    public AsyncEvaluationServiceImpl(InterviewMapper interviewMapper,
                                  AiEvaluationService aiEvaluationService,
                                  AssessmentService assessmentService,
                                  ObjectMapper objectMapper) {
        this.interviewMapper = interviewMapper;
        this.aiEvaluationService = aiEvaluationService;
        this.assessmentService = assessmentService;
        this.objectMapper = objectMapper;
    }

    /**
     * 通过虚拟线程触发异步 AI 评估。
     *
     * <p>调用后立即返回，AI 评估在虚拟线程中执行，完成后更新 interview.evaluation。</p>
     */
    public void evaluateAsync(Long interviewId, Integer manualScore) {
        log.info("[异步AI评估] 启动虚拟线程: interviewId={}", interviewId);
        Thread.startVirtualThread(() -> doEvaluate(interviewId, manualScore));
    }

    /**
     * 实际执行 AI 评估（在虚拟线程中运行）。
     */
    private void doEvaluate(Long interviewId, Integer manualScore) {
        log.info("[异步AI评估] 开始: interviewId={}", interviewId);

        // 1. 标记评估中
        interviewMapper.updateEvaluationStatus(interviewId, "PROCESSING");

        // 2. 加载面试记录
        Interview interview = interviewMapper.selectById(interviewId);
        if (interview == null) {
            log.error("[异步AI评估] 面试记录不存在: interviewId={}", interviewId);
            interviewMapper.updateEvaluationStatus(interviewId, "FAILED");
            return;
        }

        // 3. 调用 AI 评估（AgentScope2 → LLM → Heuristic 三级 fallback）
        try {
            AssessmentRequest assessReq = aiEvaluationService.evaluate(interview);

            // 4. 使用 AI 返回的维度构建 evaluation JSON，保留人工评分作为综合分
            if (manualScore != null && manualScore > 0) {
                assessReq.setOverallScore(BigDecimal.valueOf(manualScore));
            }

            Map<String, Object> evaluationMap = new LinkedHashMap<>();
            evaluationMap.put("dimensions", dimensionsFromAssessRequest(assessReq).stream()
                    .map(d -> Map.of("name", d.getName(), "weight", d.getWeight(), "score", (Object) d.getScore()))
                    .toList());
            evaluationMap.put("totalScore", assessReq.getOverallScore() != null
                    ? assessReq.getOverallScore().intValue() : 0);
            evaluationMap.put("strengths", assessReq.getStrengths());
            evaluationMap.put("weaknesses", assessReq.getWeaknesses());
            evaluationMap.put("assessedAt", DateUtils.now().toString());

            // 5. 写入 evaluation + 状态 COMPLETED
            String json = objectMapper.writeValueAsString(evaluationMap);
            interviewMapper.updateEvaluationAndStatus(interviewId, json);

            // 6. 保存到 rec_interview_assessment
            assessmentService.save(interviewId, assessReq);

            log.info("[异步AI评估] 完成: interviewId={}, score={}", interviewId,
                    assessReq.getOverallScore());
        } catch (Exception e) {
            log.error("[异步AI评估] 失败: interviewId={}", interviewId, e);
            interviewMapper.updateEvaluationStatus(interviewId, "FAILED");
        }
    }

    private List<DimensionScore> dimensionsFromAssessRequest(AssessmentRequest req) {
        return List.of(
                new DimensionScore("技术深度", 25, toIntScore(req.getTechnologyDepth())),
                new DimensionScore("沟通表达", 20, toIntScore(req.getCommunication())),
                new DimensionScore("问题解决", 20, toIntScore(req.getProblemSolving())),
                new DimensionScore("团队协作", 15, toIntScore(req.getTeamwork())),
                new DimensionScore("学习能力", 10, toIntScore(req.getLearningAbility())),
                new DimensionScore("抗压能力", 10, toIntScore(req.getTechnologyDepth()))
        );
    }

    private int toIntScore(BigDecimal bd) {
        return bd != null ? (int) Math.round(bd.doubleValue() * 20) : 60;
    }
}
