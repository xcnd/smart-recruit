package com.smartrecruit.talent.feign;

import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * Offer 服务 HTTP 接口客户端，用于获取 Offer 和入职统计数据。
 *
 * @since 1.0.0
 */
@HttpExchange("/api/v1")
public interface OfferClient {

    /** 获取 Offer 审批统计数据。 */
    @GetExchange("/offers/approvals/stats")
    ApiResponse<Map<String, Object>> getApprovalStats();

    /** 获取入职统计数据。 */
    @GetExchange("/onboarding/stats")
    ApiResponse<Map<String, Object>> getOnboardingStats();

    /** 分析：指定时间范围内的 Offer 统计。 */
    @GetExchange("/analytics/offer-stats")
    ApiResponse<Map<String, Object>> getOfferStats(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);

    /** 分析：月度 Offer 趋势。 */
    @GetExchange("/analytics/offer-trend")
    ApiResponse<List<Map<String, Object>>> getOfferTrend(
            @RequestParam(value = "months", defaultValue = "12") int months);

    /** 分析：月度入职趋势。 */
    @GetExchange("/analytics/onboarding-trend")
    ApiResponse<List<Map<String, Object>>> getOnboardingTrend(
            @RequestParam(value = "months", defaultValue = "12") int months);

    /** 分析：Offer/入职日汇总（定时任务预聚合）。 */
    @GetExchange("/analytics/offer-daily")
    ApiResponse<List<Map<String, Object>>> getOfferDaily(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate);
}
