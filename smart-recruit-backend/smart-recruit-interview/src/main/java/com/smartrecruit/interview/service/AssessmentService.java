package com.smartrecruit.interview.service;

import com.smartrecruit.interview.dto.request.AssessmentRequest;
import com.smartrecruit.interview.dto.response.AssessmentResponse;

/**
 * AI 评估报告服务接口。
 *
 * @since 1.0.0
 */
public interface AssessmentService {

    /**
     * 根据面试 ID 获取评估报告。
     *
     * @param interviewId 面试 ID
     * @return 评估报告详情
     * @throws com.smartrecruit.common.exception.ResourceNotFoundException 如果评估报告不存在
     */
    AssessmentResponse getByInterviewId(Long interviewId);

    /**
     * 创建或更新 AI 评估报告。
     * <p>如果指定面试已存在评估则更新，否则创建新记录。</p>
     *
     * @param interviewId 面试 ID
     * @param request     评估数据
     * @return 保存后的评估报告
     */
    AssessmentResponse save(Long interviewId, AssessmentRequest request);
}
