package com.smartrecruit.interview.service;

import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;

import java.util.List;

/**
 * 面试题库服务，按测评类型提供题目与结果说明。
 *
 * @since 1.0.0
 */
public interface QuestionBankService {

    /**
     * 按类型获取指定数量的测评题目。
     */
    List<AssessmentQuestionItem> getQuestions(Integer type, int count);

    /**
     * 根据得分生成测评结果说明。
     */
    String getResultDescription(Integer type, String score, int total, int correct);
}
