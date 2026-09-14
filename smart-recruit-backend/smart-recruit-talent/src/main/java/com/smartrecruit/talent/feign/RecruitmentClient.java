package com.smartrecruit.talent.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.talent.dto.remote.CandidateDTO;
import com.smartrecruit.talent.dto.remote.JobDetailDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 招聘服务 HTTP 接口客户端，用于获取候选人、职位、工作台统计等信息。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface RecruitmentClient {

    @GetExchange("/candidates/{id}")
    ApiResponse<CandidateDTO> getCandidate(@PathVariable("id") Long id);

    @GetExchange("/jobs/{id}")
    ApiResponse<JobDetailDTO> getJob(@PathVariable("id") Long id);

    /** 工作台 KPI 统计。 */
    @GetExchange("/dashboard/kpi")
    ApiResponse<Map<String, Object>> getDashboardKpi();

    /** 招聘漏斗数据。 */
    @GetExchange("/dashboard/funnel")
    ApiResponse<List<Map<String, Object>>> getFunnel();

    /** 部门招聘进度。 */
    @GetExchange("/dashboard/department-progress")
    ApiResponse<List<Map<String, Object>>> getDepartmentProgress();

    /** 最近活动动态。 */
    @GetExchange("/activities/recent")
    ApiResponse<List<Map<String, Object>>> getRecentActivities(@RequestParam("limit") int limit);

    /** 待办任务列表。 */
    @GetExchange("/workbench/pending")
    ApiResponse<List<Map<String, Object>>> getPendingTasks(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", defaultValue = "20") int limit);

    /** 完成待办任务。 */
    @PutExchange("/workbench/pending/{id}/complete")
    ApiResponse<Void> completeTask(@PathVariable("id") Long id);

    /** 忽略待办任务。 */
    @PutExchange("/workbench/pending/{id}/dismiss")
    ApiResponse<Void> dismissTask(@PathVariable("id") Long id);

    /** 分页查询活动动态。 */
    @GetExchange("/activities")
    ApiResponse<Map<String, Object>> getActivitiesPage(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "keyword", required = false) String keyword);

    /** 分页查询待办任务。 */
    @GetExchange("/workbench/tasks")
    ApiResponse<Map<String, Object>> getTasksPage(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "priority", required = false) Integer priority,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword);

    /** 分析：指定时间范围内新增候选人的阶段分布。 */
    @GetExchange("/analytics/funnel")
    ApiResponse<List<Map<String, Object>>> getAnalyticsFunnel(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);

    /** 分析：指定时间范围内新增候选人的渠道分布。 */
    @GetExchange("/analytics/channels")
    ApiResponse<List<Map<String, Object>>> getAnalyticsChannels(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);

    /** 分析：月度新增候选人趋势。 */
    @GetExchange("/analytics/candidate-trend")
    ApiResponse<List<Map<String, Object>>> getCandidateTrend(
            @RequestParam(value = "months", defaultValue = "12") int months);

    /** 分析：候选人日汇总（定时任务预聚合，按日期返回阶段 × 来源聚合行）。 */
    @GetExchange("/analytics/candidate-daily")
    ApiResponse<List<Map<String, Object>>> getCandidateDaily(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);
}
