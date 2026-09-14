package com.smartrecruit.offer.service;

import com.smartrecruit.offer.dto.response.OfferAnalyticsVO;
import com.smartrecruit.offer.dto.response.OfferTrendVO;
import com.smartrecruit.offer.dto.response.OnboardingTrendVO;

import java.time.LocalDate;
import java.util.List;

/**
 * Offer 分析服务接口。
 *
 * @since 2026-04-05
 */
public interface OfferAnalyticsService {

    /**
     * 统计指定时间范围内的 Offer 数据（按发送时间）。
     */
    OfferAnalyticsVO getOfferStats(LocalDate startDate, LocalDate endDate);

    /**
     * 月度 Offer 趋势（最近 N 个月，按发送时间）。
     */
    List<OfferTrendVO> getOfferTrend(int months);

    /**
     * 月度入职趋势（最近 N 个月）。
     */
    List<OnboardingTrendVO> getOnboardingTrend(int months);
}
