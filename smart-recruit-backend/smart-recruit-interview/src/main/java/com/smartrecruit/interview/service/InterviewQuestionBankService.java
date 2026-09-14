package com.smartrecruit.interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.QuestionBankSaveRequest;
import com.smartrecruit.interview.dto.response.QuestionBankVO;

import java.util.Map;

/**
 * 面试题库服务。
 *
 * @since 2026-04-10
 */
public interface InterviewQuestionBankService {

    /**
     * 分页查询套题（支持部门/职位/关键词/状态过滤）。
     */
    PageResult<QuestionBankVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 查询套题详情（含题目列表）。
     */
    QuestionBankVO getById(Long id);

    /**
     * 新建套题（含题目）。
     */
    QuestionBankVO create(QuestionBankSaveRequest request);

    /**
     * 更新套题（题目整体替换）。
     */
    QuestionBankVO update(Long id, QuestionBankSaveRequest request);

    /**
     * 删除套题（逻辑删除套题与题目）。
     */
    void delete(Long id);
}
