package com.smartrecruit.interview.service;

import com.smartrecruit.interview.dto.request.QuestionGenerateRequest;
import com.smartrecruit.interview.dto.request.SendQuestionsEmailRequest;
import com.smartrecruit.interview.dto.response.QuestionGenerateResult;
import com.smartrecruit.interview.dto.response.QuestionGenerateTaskVO;
import com.smartrecruit.interview.dto.response.CompletedQuestionResultVO;

import java.util.List;
import java.util.Map;

/**
 * AI 智能出题服务接口。
 *
 * @since 1.0.0
 */
public interface QuestionService {

    /**
     * 根据岗位类型生成分组面试题（同步，已废弃，请使用 {@link #generateAsync}）。
     *
     * @deprecated 建议使用 {@link #generateAsync(QuestionGenerateRequest)} 异步出题
     */
    @Deprecated
    QuestionGenerateResult generate(QuestionGenerateRequest request);

    /**
     * 异步出题 — 立即返回任务 ID，后台调用大模型生成题目。
     *
     * @param request 包含岗位类型编码的请求
     * @return 任务 ID
     */
    String generateAsync(QuestionGenerateRequest request);

    /**
     * 根据任务 ID 查询异步出题任务的状态和结果。
     *
     * @param taskId 任务 ID
     * @return 任务状态 VO
     */
    QuestionGenerateTaskVO getGenerateResult(String taskId);

    /**
     * 将生成的面试题通过邮件发送给面试官。
     *
     * @param request 包含面试官邮箱、职位名称和题目列表
     */
    void sendQuestionsByEmail(SendQuestionsEmailRequest request);

    /**
     * 查询所有已完成的出题任务摘要（供测评创建时选择已有出题结果）。
     *
     * @return 已完成任务列表，每项包含 taskId、positionLabel、positionType
     */
    List<CompletedQuestionResultVO> listCompletedResults();
}
