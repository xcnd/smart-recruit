package com.smartrecruit.offer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.offer.dto.response.OfferAnalyticsVO;
import com.smartrecruit.offer.dto.response.OfferTrendVO;
import com.smartrecruit.offer.dto.response.OnboardingTrendVO;
import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.entity.Onboarding;
import com.smartrecruit.offer.enums.OfferEnums;
import com.smartrecruit.offer.repository.OfferMapper;
import com.smartrecruit.offer.repository.OnboardingMapper;
import com.smartrecruit.offer.service.OfferAnalyticsService;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offer 分析服务实现，基于 Offer 与入职真实数据统计。
 *
 * @since 2026-04-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OfferAnalyticsServiceImpl implements OfferAnalyticsService {

    private static final int MAX_TREND_MONTHS = 24;

    private final OfferMapper offerMapper;
    private final OnboardingMapper onboardingMapper;

    /** 查询 Offer 统计信息。 */
    @Override
    public OfferAnalyticsVO getOfferStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.plusDays(1).atStartOfDay();

        List<Offer> offers = offerMapper.selectList(offerRangeWrapper(start, end));
        long sent = offers.size();
        long accepted = offers.stream().filter(o -> OfferEnums.OfferStatus.ACCEPTED.getCode() == o.getStatus()).count();
        long declined = offers.stream().filter(o -> OfferEnums.OfferStatus.REJECTED.getCode() == o.getStatus()).count();
        long pending = offers.stream().filter(o -> OfferEnums.OfferStatus.SENT.getCode() == o.getStatus()).count();
        long onboardCount = onboardingMapper.selectCount(
                new LambdaQueryWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0)
                        .eq(Onboarding::getStatus, OfferEnums.OnboardingStatus.DONE.getCode())
                        .ge(start != null, Onboarding::getCreateTime, start)
                        .lt(end != null, Onboarding::getCreateTime, end));

        double acceptanceRate = accepted + declined > 0
                ? Math.round(accepted * 10000.0 / (accepted + declined)) / 100.0 : 0.0;
        double avgConfirmDays = avgConfirmDays(offers);

        return OfferAnalyticsVO.builder()
                .sentCount(sent)
                .acceptedCount(accepted)
                .declinedCount(declined)
                .pendingCount(pending)
                .onboardCount(onboardCount)
                .acceptanceRate(acceptanceRate)
                .avgConfirmDays(avgConfirmDays)
                .build();
    }

    /** 查询 Offer 发送/确认趋势。 */
    @Override
    public List<OfferTrendVO> getOfferTrend(int months) {
        int actualMonths = Math.max(1, Math.min(months, MAX_TREND_MONTHS));
        YearMonth current = DateUtils.currentYearMonth();
        LocalDateTime start = current.minusMonths(actualMonths - 1L).atDay(1).atStartOfDay();

        List<Offer> offers = offerMapper.selectList(
                new LambdaQueryWrapper<Offer>()
                        .eq(Offer::getDeleted, 0)
                        .ge(Offer::getSendTime, start));

        Map<YearMonth, List<Offer>> byMonth = new HashMap<>();
        for (Offer offer : offers) {
            if (offer.getSendTime() != null) {
                byMonth.computeIfAbsent(YearMonth.from(offer.getSendTime()), k -> new ArrayList<>())
                        .add(offer);
            }
        }

        List<OfferTrendVO> trend = new ArrayList<>();
        for (int i = actualMonths - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            List<Offer> monthOffers = byMonth.getOrDefault(month, List.of());
            long accepted = monthOffers.stream()
                    .filter(o -> OfferEnums.OfferStatus.ACCEPTED.getCode() == o.getStatus()).count();
            trend.add(OfferTrendVO.builder()
                    .month(month.toString())
                    .sentCount((long) monthOffers.size())
                    .acceptedCount(accepted)
                    .avgConfirmDays(avgConfirmDays(monthOffers))
                    .build());
        }
        return trend;
    }

    /** 查询入职趋势。 */
    @Override
    public List<OnboardingTrendVO> getOnboardingTrend(int months) {
        int actualMonths = Math.max(1, Math.min(months, MAX_TREND_MONTHS));
        YearMonth current = DateUtils.currentYearMonth();
        LocalDateTime start = current.minusMonths(actualMonths - 1L).atDay(1).atStartOfDay();

        List<Onboarding> onboardings = onboardingMapper.selectList(
                new LambdaQueryWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0)
                        .eq(Onboarding::getStatus, OfferEnums.OnboardingStatus.DONE.getCode())
                        .ge(Onboarding::getCreateTime, start));

        Map<YearMonth, Long> byMonth = new HashMap<>();
        for (Onboarding onboarding : onboardings) {
            LocalDateTime base = onboarding.getActualOnboardDate() != null
                    ? onboarding.getActualOnboardDate().atStartOfDay()
                    : onboarding.getCreateTime();
            if (base != null) {
                byMonth.merge(YearMonth.from(base), 1L, Long::sum);
            }
        }

        List<OnboardingTrendVO> trend = new ArrayList<>();
        for (int i = actualMonths - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            trend.add(OnboardingTrendVO.builder()
                    .month(month.toString())
                    .onboardCount(byMonth.getOrDefault(month, 0L))
                    .build());
        }
        return trend;
    }

    private LambdaQueryWrapper<Offer> offerRangeWrapper(LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<Offer> wrapper = new LambdaQueryWrapper<Offer>()
                .eq(Offer::getDeleted, 0)
                .isNotNull(Offer::getSendTime);
        if (start != null) {
            wrapper.ge(Offer::getSendTime, start);
        }
        if (end != null) {
            wrapper.lt(Offer::getSendTime, end);
        }
        return wrapper;
    }

    private double avgConfirmDays(List<Offer> offers) {
        long totalDays = 0;
        int counted = 0;
        for (Offer offer : offers) {
            if (offer.getSendTime() != null && offer.getRespondTime() != null
                    && offer.getRespondTime().isAfter(offer.getSendTime())) {
                totalDays += DateUtils.daysBetween(offer.getSendTime().toLocalDate(),
                        offer.getRespondTime().toLocalDate());
                counted++;
            }
        }
        return counted > 0 ? Math.round(totalDays * 10.0 / counted) / 10.0 : 0.0;
    }
}
