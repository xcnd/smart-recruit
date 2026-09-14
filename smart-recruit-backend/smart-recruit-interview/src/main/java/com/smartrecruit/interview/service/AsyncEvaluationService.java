package com.smartrecruit.interview.service;

/**
 * 异步面试评估服务。
 *
 * @since 1.0.0
 */
public interface AsyncEvaluationService {

    /**
     * 异步触发面试评估。
     *
     * @param interviewId 面试 ID
     * @param manualScore 人工评分（可为 null，使用 AI 评分）
     */
    void evaluateAsync(Long interviewId, Integer manualScore);
}
