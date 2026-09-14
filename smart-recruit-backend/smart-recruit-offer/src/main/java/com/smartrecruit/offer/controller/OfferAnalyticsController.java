package com.smartrecruit.offer.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.offer.dto.response.OfferAnalyticsVO;
import com.smartrecruit.offer.dto.response.OfferDailyStatVO;
import com.smartrecruit.offer.dto.response.OfferTrendVO;
import com.smartrecruit.offer.dto.response.OnboardingTrendVO;
import com.smartrecruit.offer.entity.OfferDailyStats;
import com.smartrecruit.offer.repository.OfferDailyStatsMapper;
import com.smartrecruit.offer.service.OfferAnalyticsService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Offer 分析数据控制器（内部服务）。
 *
 * @since 2026-04-05
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class OfferAnalyticsController {

    private final OfferAnalyticsService offerAnalyticsService;
    private final OfferDailyStatsMapper offerDailyStatsMapper;

    /**
     * 查询 Offer 核心统计（发送数、接受率、平均确认周期等）。
     */
    @GetMapping("/offer-stats")
    public ApiResponse<OfferAnalyticsVO> offerStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ApiResponse.success(offerAnalyticsService.getOfferStats(
                DateUtils.parseDate(startDate), DateUtils.parseDate(endDate)));
    }

    /**
     * 查询 Offer 发送/确认月度趋势。
     */
    @GetMapping("/offer-trend")
    public ApiResponse<List<OfferTrendVO>> offerTrend(
            @RequestParam(value = "months", defaultValue = "12") int months) {
        return ApiResponse.success(offerAnalyticsService.getOfferTrend(months));
    }

    /**
     * 查询入职月度趋势。
     */
    @GetMapping("/onboarding-trend")
    public ApiResponse<List<OnboardingTrendVO>> onboardingTrend(
            @RequestParam(value = "months", defaultValue = "12") int months) {
        return ApiResponse.success(offerAnalyticsService.getOnboardingTrend(months));
    }

    /**
     * Offer/入职日汇总查询（内部服务）：按日期返回聚合行。
     *
     * <p>分析页只读本接口，数据由定时任务预聚合。</p>
     */
    @GetMapping("/offer-daily")
    public ApiResponse<List<OfferDailyStatVO>> offerDaily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LambdaQueryWrapper<OfferDailyStats> wrapper = new LambdaQueryWrapper<>();
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(OfferDailyStats::getStatDate, DateUtils.parseDate(startDate));
        }
        if (endDate != null && !endDate.isBlank()) {
            LocalDate endExclusive = DateUtils.parseDate(endDate);
            wrapper.lt(OfferDailyStats::getStatDate, endExclusive.plusDays(1));
        }
        wrapper.orderByAsc(OfferDailyStats::getStatDate);

        List<OfferDailyStatVO> result = offerDailyStatsMapper.selectList(wrapper).stream()
                .map(s -> new OfferDailyStatVO(
                        s.getStatDate(), s.getSentCount(), s.getAcceptedCount(), s.getDeclinedCount(),
                        s.getPendingCount(), s.getOnboardCount(), s.getConfirmTotalDays(), s.getConfirmCount()))
                .toList();
        return ApiResponse.success(result);
    }
}
