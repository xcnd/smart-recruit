package com.smartrecruit.interview.service;

import com.smartrecruit.interview.dto.request.SubmitAnswersRequest;
import com.smartrecruit.interview.dto.response.AssessmentPageVO;
import com.smartrecruit.interview.dto.response.AssessResultVO;

/**
 * 公开测评服务 — 候选人通过邮件链接中的 token 访问测评页面。
 *
 * @since 1.0.0
 */
public interface PublicAssessmentService {

    /**
     * 通过测评 token 获取测评页面数据（试题 + 候选人信息）。
     */
    AssessmentPageVO getAssessmentByToken(String token);

    /**
     * 提交测评答案并获取成绩。
     */
    AssessResultVO submitAssessment(SubmitAnswersRequest request);
}
