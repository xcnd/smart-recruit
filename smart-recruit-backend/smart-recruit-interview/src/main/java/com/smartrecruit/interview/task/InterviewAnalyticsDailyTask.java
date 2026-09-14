package com.smartrecruit.interview.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.interview.entity.Interview;
import com.smartrecruit.interview.entity.InterviewDailyStats;
import com.smartrecruit.interview.repository.InterviewDailyStatsMapper;
import com.smartrecruit.interview.repository.InterviewMapper;
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
 * 面试日汇总定时任务。
 *
 * <p>每 30 分钟刷新"昨天 + 今天"的聚合数据；首次运行时全量回填近 400 天。
 * 分析页查询只读 {@code analytics_interview_daily}。</p>
 *
 * @since 2026-04-06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewAnalyticsDailyTask {

    private static final int BACKFILL_DAYS = 400;
    private static final int REFRESH_DAYS = 2;

    private final InterviewMapper interviewMapper;
    private final InterviewDailyStatsMapper dailyStatsMapper;

    /**
     * 执行面试日汇总：表为空时全量回填近 400 天，否则仅刷新最近 2 天。
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
        log.info("面试日汇总完成: days={}, refreshed={}", days, refreshed);
    }

    private int aggregateDay(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        dailyStatsMapper.delete(new LambdaQueryWrapper<InterviewDailyStats>()
                .eq(InterviewDailyStats::getStatDate, day));

        List<Interview> interviews = interviewMapper.selectList(
                new LambdaQueryWrapper<Interview>()
                        .eq(Interview::getDeleted, 0)
                        .ge(Interview::getCreateTime, start)
                        .lt(Interview::getCreateTime, end));

        int passed = 0, cancelled = 0;
        for (Interview interview : interviews) {
            boolean isCancelled = interview.getStatus() != null && interview.getStatus() == 3;
            if (isCancelled) {
                cancelled++;
            }
            // 已取消的面试不计入通过数
            if (!isCancelled && interview.getResult() != null && interview.getResult() == 0) {
                passed++;
            }
        }

        InterviewDailyStats stats = new InterviewDailyStats();
        stats.setStatDate(day);
        stats.setTotalCount(interviews.size());
        stats.setPassedCount(passed);
        stats.setCancelledCount(cancelled);
        dailyStatsMapper.insert(stats);
        return 1;
    }
}
