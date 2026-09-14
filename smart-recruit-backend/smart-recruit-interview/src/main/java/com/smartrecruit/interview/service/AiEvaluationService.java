package com.smartrecruit.interview.service;

import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.response.InterviewReportVO.DimensionScore;
import com.smartrecruit.interview.entity.Interview;

import java.util.List;

/**
 * AI 面试评估服务 — 调用大模型生成六维度评估报告。
 *
 * <p>LLM 启用时调用真实大模型，禁用或失败时降级为启发式模拟评分。</p>
 *
 * @since 1.0.0
 */
public interface AiEvaluationService {

    /**
     * 评估面试并返回结构化评估请求对象。
     *
     * <p>优先级：AgentScope2 HarnessAgent > LangChain4j LLM 网关 > 启发式评分降级</p>
     */
    AssessmentRequest evaluate(Interview interview);

    /**
     * 使用启发式评分生成面试评估（LLM 不可用时的降级方案）。
     */
    AssessmentRequest evaluateWithHeuristic(Interview interview);

    /**
     * 生成六维度评分列表（启发式随机评分）。
     */
    List<DimensionScore> generateDimensionScores();
}
