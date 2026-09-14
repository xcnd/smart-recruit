package com.smartrecruit.interview.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartrecruit.common.dto.PageResult;
import com.smartrecruit.interview.dto.request.CreateInterviewRequest;
import com.smartrecruit.interview.dto.request.SubmitInterviewRequest;
import com.smartrecruit.interview.dto.request.UpdateInterviewRequest;
import com.smartrecruit.interview.dto.request.InterviewFeedbackRequest;
import com.smartrecruit.interview.dto.response.InterviewDetailVO;
import com.smartrecruit.interview.dto.response.InterviewReportVO;
import com.smartrecruit.interview.dto.response.InterviewStatsVO;
import com.smartrecruit.interview.dto.response.InterviewScheduleVO;
import com.smartrecruit.interview.dto.response.InterviewVO;
import com.smartrecruit.interview.dto.response.AiStatsVO;
import com.smartrecruit.interview.dto.response.AssessedCandidateVO;
import com.smartrecruit.interview.dto.response.NextRoundVO;

import java.util.List;
import java.util.Map;

/**
 * 面试管理服务接口。
 *
 * @since 1.0.0
 */
public interface InterviewService {

    /**
     * 分页查询，支持可选过滤参数。
     *
     * @param page   分页信息
     * @param params 过滤参数（candidateId, jobPositionId, status, type）
     * @return InterviewVO 分页结果
     */
    PageResult<InterviewVO> pageQuery(Page<?> page, Map<String, Object> params);

    /**
     * 创建新面试并生成 AI 问题模板。
     *
     * @param request 创建面试请求
     * @return 创建后的 InterviewVO
     */
    InterviewVO create(CreateInterviewRequest request);

    /**
     * 根据 ID 获取面试详情，包含 AI 问题。
     *
     * @param id 面试 ID
     * @return InterviewDetailVO
     */
    InterviewDetailVO getById(Long id);

    /**
     * 查询面试的 AI 面试题目列表（按行拆分）。
     *
     * @param id 面试 ID
     * @return 题目列表；未生成或为空时返回空列表
     */
    List<String> getAiQuestions(Long id);

    /**
     * 获取六维度面试报告，用于雷达图可视化。
     *
     * @param id 面试 ID
     * @return 包含维度评分的 InterviewReportVO
     */
    InterviewReportVO getReport(Long id);

    /**
     * 提交面试结果，将状态更新为已完成并生成模拟 AI 报告评分。
     *
     * @param id      面试 ID
     * @param request 提交请求（含反馈信息）
     */
    void submitResult(Long id, SubmitInterviewRequest request);

    /**
     * 取消已安排的面试。
     *
     * @param id 面试 ID
     */
    void cancel(Long id);

    /**
     * 更新面试信息（仅更新前端传入的非空字段）。
     *
     * @param id      面试 ID
     * @param request 更新请求
     * @return 更新后的 InterviewVO
     */
    InterviewVO update(Long id, UpdateInterviewRequest request);

    /**
     * 获取面试统计数据（总数、今日、已通过、已取消）。
     *
     * @return InterviewStatsVO
     */
    InterviewStatsVO getStats();

    /**
     * 查询指定时间范围内的面试统计。
     *
     * @param start 起始时间（含），可为 null
     * @param end   结束时间（不含），可为 null
     */
    InterviewStatsVO getStats(java.time.LocalDateTime start, java.time.LocalDateTime end);

    /**
     * 删除面试（逻辑删除，标记 deleted=1）。
     *
     * @param id 面试 ID
     */
    void delete(Long id);

    /**
     * 获取指定日期的面试日程列表。
     *
     * @param date 日期字符串（yyyy-MM-dd）
     * @return 日程列表，按时段排序
     */
    List<InterviewScheduleVO> getSchedule(String date);

    /**
     * 获取日期范围内的面试日程列表（周视图）。
     *
     * @param startDate 起始日期（yyyy-MM-dd）
     * @param endDate   结束日期（yyyy-MM-dd）
     * @return 日程列表，按时段排序
     */
    List<InterviewScheduleVO> getWeekSchedule(String startDate, String endDate);

    /**
     * 获取 AI 智能面试页面的统计卡片数据。
     *
     * @return AiStatsVO（今日面试数、已完成、AI评估完成数、通过率）
     */
    AiStatsVO getAiStats();

    /**
     * 查询已评估的候选人列表（用于 AI 评估报告下拉选择）。
     *
     * @return 已评估候选人简要信息列表
     */
    List<AssessedCandidateVO> getAssessedCandidates();

    /**
     * 提交面试官维度反馈（五维星评 + 评语 + 录用建议）。
     *
     * @param id      面试 ID
     * @param request 反馈请求
     */
    void submitFeedback(Long id, InterviewFeedbackRequest request);

    /**
     * 开始面试，将状态从已安排变更为进行中。
     *
     * @param id 面试 ID
     */
    void startInterview(Long id);

    /**
     * 更新面试结果（通过/淘汰/待定），支持设置评分和反馈。
     *
     * @param id       面试 ID
     * @param result   结果编码：0=通过,1=淘汰,2=待定
     * @param score    综合评分（0-100），可选
     * @param feedback 面试官反馈评语，可选
     */
    void updateResult(Long id, Integer result, Integer score, String feedback);

    /**
     * 查询候选人的建议下一轮面试轮次。
     *
     * @param candidateId 候选人ID
     * @return { nextRound, existingRounds }
     */
    NextRoundVO getSuggestedNextRound(Long candidateId);
}
