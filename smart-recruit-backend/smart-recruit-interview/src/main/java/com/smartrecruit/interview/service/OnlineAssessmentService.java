package com.smartrecruit.interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.CreateAssessmentRequest;
import com.smartrecruit.interview.dto.response.AssessmentQuestionItem;
import com.smartrecruit.interview.dto.response.OnlineAssessmentVO;

import java.util.List;
import java.util.Map;

/**
 * 在线测评管理服务接口。
 *
 * @since 1.0.0
 */
public interface OnlineAssessmentService {

    /**
     * 分页查询在线测评列表。
     *
     * @param page   分页参数
     * @param params 筛选参数（candidateId, status）
     * @return 分页结果
     */
    PageResult<OnlineAssessmentVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 创建新的在线测评。
     *
     * @param request 创建请求
     * @return 创建后的 VO
     */
    OnlineAssessmentVO create(CreateAssessmentRequest request);

    /**
     * 发送测评给候选人（状态变为待完成）。
     *
     * @param id 测评 ID
     */
    void send(Long id);

    /**
     * 更新测评成绩。
     *
     * @param id    测评 ID
     * @param score 成绩
     */
    void updateScore(Long id, String score);

    /**
     * 为已有测评生成 AI 编程测试题目（仅限 type=0 且未发送的测评）。
     * 调用 LLM 生成 10 道岗位相关的单项选择题，存入 questionsJson 字段。
     *
     * @param id 测评 ID
     */
    void generateQuestions(Long id);

    /**
     * 查询测评题目详情（含正确答案，仅内部管理端调用）。
     * 优先返回 AI 生成的题目，否则回退到硬编码题库。
     *
     * @param id 测评 ID
     * @return 题目列表
     */
    List<AssessmentQuestionItem> getQuestions(Long id);

    /**
     * 直接保存测评题目（将题目列表序列化为 JSON 存入 questionsJson 字段）。
     * 用于将 AI 智能出题生成的开放式问答题保存为测评题目。
     *
     * @param id        测评 ID
     * @param questions 题目列表
     */
    void saveQuestions(Long id, List<AssessmentQuestionItem> questions);
}
