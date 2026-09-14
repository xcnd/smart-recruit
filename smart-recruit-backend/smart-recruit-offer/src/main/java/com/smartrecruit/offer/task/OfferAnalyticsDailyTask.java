package com.smartrecruit.offer.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.offer.entity.Offer;
import com.smartrecruit.offer.entity.OfferDailyStats;
import com.smartrecruit.offer.entity.Onboarding;
import com.smartrecruit.offer.enums.OfferEnums;
import com.smartrecruit.offer.repository.OfferDailyStatsMapper;
import com.smartrecruit.offer.repository.OfferMapper;
import com.smartrecruit.offer.repository.OnboardingMapper;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Offer/入职日汇总定时任务。
 *
 * <p>每 30 分钟刷新"昨天 + 今天"的聚合数据；首次运行时全量回填近 400 天，
 * 保证 12 个月趋势图有历史数据。分析页查询只读 {@code analytics_offer_daily}。</p>
 *
 * @since 2026-04-06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OfferAnalyticsDailyTask {

    private static final int BACKFILL_DAYS = 400;
    private static final int REFRESH_DAYS = 2;

    private final OfferMapper offerMapper;
    private final OnboardingMapper onboardingMapper;
    private final OfferDailyStatsMapper dailyStatsMapper;

    /**
     * 执行 Offer/入职日汇总：表为空时全量回填近 400 天，否则仅刷新最近 2 天。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDaily() {
        long existing = dailyStatsMapper.selectCount(null);
        int days = existing == 0 ? BACKFILL_DAYS : REFRESH_DAYS;

        LocalDate today = DateUtils.today();
        int refreshed = 0;
        for (int i = days - 1; i >= 0; i--) {
            refreshed += aggregateDay(today.minusDays(i));
        }
        log.info("Offer 日汇总完成: days={}, refreshed={}", days, refreshed);
    }

    private int aggregateDay(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        dailyStatsMapper.delete(new LambdaQueryWrapper<OfferDailyStats>()
                .eq(OfferDailyStats::getStatDate, day));

        List<Offer> offers = offerMapper.selectList(
                new LambdaQueryWrapper<Offer>()
                        .eq(Offer::getDeleted, 0)
                        .isNotNull(Offer::getSendTime)
                        .ge(Offer::getSendTime, start)
                        .lt(Offer::getSendTime, end));

        int sent = offers.size();
        int accepted = 0, declined = 0, pending = 0;
        long confirmTotalDays = 0L;
        int confirmCount = 0;
        for (Offer offer : offers) {
            int status = offer.getStatus() != null ? offer.getStatus() : -1;
            if (OfferEnums.OfferStatus.ACCEPTED.getCode() == status) accepted++;
            else if (OfferEnums.OfferStatus.REJECTED.getCode() == status) declined++;
            else if (OfferEnums.OfferStatus.SENT.getCode() == status) pending++;

            if (offer.getSendTime() != null && offer.getRespondTime() != null
                    && offer.getRespondTime().isAfter(offer.getSendTime())) {
                confirmTotalDays += DateUtils.daysBetween(
                        offer.getSendTime().toLocalDate(), offer.getRespondTime().toLocalDate());
                confirmCount++;
            }
        }

        long onboardCount = onboardingMapper.selectCount(
                new LambdaQueryWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0)
                        .eq(Onboarding::getStatus, OfferEnums.OnboardingStatus.DONE.getCode())
                        .eq(Onboarding::getActualOnboardDate, day));
        onboardCount += onboardingMapper.selectCount(
                new LambdaQueryWrapper<Onboarding>()
                        .eq(Onboarding::getDeleted, 0)
                        .eq(Onboarding::getStatus, OfferEnums.OnboardingStatus.DONE.getCode())
                        .isNull(Onboarding::getActualOnboardDate)
                        .ge(Onboarding::getCreateTime, start)
                        .lt(Onboarding::getCreateTime, end));

        OfferDailyStats stats = new OfferDailyStats();
        stats.setStatDate(day);
        stats.setSentCount(sent);
        stats.setAcceptedCount(accepted);
        stats.setDeclinedCount(declined);
        stats.setPendingCount(pending);
        stats.setOnboardCount((int) onboardCount);
        stats.setConfirmTotalDays(confirmTotalDays);
        stats.setConfirmCount(confirmCount);
        dailyStatsMapper.insert(stats);
        return 1;
    }
}
