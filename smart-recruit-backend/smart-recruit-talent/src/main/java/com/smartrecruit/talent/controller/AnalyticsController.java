package com.smartrecruit.talent.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.talent.dto.response.AnalyticsOverviewVO;
import com.smartrecruit.talent.dto.response.ChannelVO;
import com.smartrecruit.talent.dto.response.FunnelStageVO;
import com.smartrecruit.talent.dto.response.InsightVO;
import com.smartrecruit.talent.dto.response.RecruitmentCycleVO;
import com.smartrecruit.talent.service.AnalyticsService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 招聘分析 REST 控制器。
 *
 * <p>端点：漏斗、渠道、洞察、导出。</p>
 *
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * 分析页聚合数据：KPI、漏斗、渠道、趋势、洞察。
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<AnalyticsOverviewVO> overview(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Analytics overview requested: {} ~ {}", startDate, endDate);
        return ApiResponse.success(analyticsService.getOverview(
                DateUtils.parseDate(startDate), DateUtils.parseDate(endDate)));
    }

    /**
     * 查询招聘漏斗数据。
     */
    @GetMapping("/funnel")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<List<FunnelStageVO>> funnel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Analytics funnel requested: {} ~ {}", startDate, endDate);
        List<FunnelStageVO> funnel = analyticsService.getFunnel(
                DateUtils.parseDate(startDate), DateUtils.parseDate(endDate));
        return ApiResponse.success(funnel);
    }

    /**
     * 查询渠道分析数据。
     */
    @GetMapping("/channels")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<List<ChannelVO>> channels(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Channel analysis requested: {} ~ {}", startDate, endDate);
        List<ChannelVO> channels = analyticsService.getChannelAnalysis(
                DateUtils.parseDate(startDate), DateUtils.parseDate(endDate));
        return ApiResponse.success(channels);
    }

    /**
     * 查询 AI 生成的招聘洞察建议。
     */
    @GetMapping("/insights")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<List<InsightVO>> insights() {
        log.info("AI insights requested");
        List<InsightVO> insights = analyticsService.getInsights();
        return ApiResponse.success(insights);
    }

    /**
     * 查询招聘周期（Offer 发送→确认）趋势数据。
     */
    @GetMapping("/recruitment-cycle")
    @PreAuthorize("hasAuthority('analytics:view')")
    public ApiResponse<List<RecruitmentCycleVO>> recruitmentCycle(
            @RequestParam(required = false) String period) {
        log.info("Recruitment cycle requested: period={}", period);
        List<RecruitmentCycleVO> cycle = analyticsService.getRecruitmentCycle(period);
        return ApiResponse.success(cycle);
    }

    /**
     * 导出分析报表（CSV 文件下载）。
     */
    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("Analytics report export requested: {} ~ {}", startDate, endDate);
        byte[] report = analyticsService.exportReport(
                DateUtils.parseDate(startDate), DateUtils.parseDate(endDate));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=smart-recruit-analytics-report.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(report);
    }

}
