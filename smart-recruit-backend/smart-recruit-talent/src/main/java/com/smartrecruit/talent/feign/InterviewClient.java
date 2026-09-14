package com.smartrecruit.talent.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * 面试服务 HTTP 接口客户端，用于获取面试统计数据。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface InterviewClient {

    /** 获取面试统计数据，返回 status 分布计数。 */
    @GetExchange("/interviews/stats")
    ApiResponse<Map<String, Object>> getStats();

    /** 获取指定时间范围内的面试统计数据。 */
    @GetExchange("/interviews/stats")
    ApiResponse<Map<String, Object>> getStats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);

    /** 获取指定状态的面试数量（如 SCHEDULED/IN_PROGRESS）。 */
    @GetExchange("/interviews/count")
    ApiResponse<Long> countByStatus(@RequestParam("status") String status);

    /** 分析：面试日汇总（定时任务预聚合）。 */
    @GetExchange("/analytics/interview-daily")
    ApiResponse<List<Map<String, Object>>> getInterviewDaily(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);
}
