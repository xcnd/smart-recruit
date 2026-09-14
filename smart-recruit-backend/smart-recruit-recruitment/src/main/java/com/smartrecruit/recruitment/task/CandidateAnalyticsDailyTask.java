package com.smartrecruit.recruitment.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.recruitment.entity.CandidateDailyStats;
import com.smartrecruit.recruitment.repository.CandidateDailyStatsMapper;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.common.util.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 候选人日汇总定时任务。
 *
 * <p>每 30 分钟刷新"昨天 + 今天"的聚合数据；首次运行时全量回填近 400 天，
 * 保证 12 个月趋势图有历史数据。分析页查询只读 {@code analytics_candidate_daily}，
 * 避免对 {@code rec_candidate} 大表实时 GROUP BY。</p>
 *
 * @since 2026-04-06
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CandidateAnalyticsDailyTask {

    /** 首次全量回填天数（覆盖 12 个月趋势 + 余量）。 */
    private static final int BACKFILL_DAYS = 400;

    /** 常规刷新天数：昨天 + 今天（当天数据随时间增长，需重复刷新）。 */
    private static final int REFRESH_DAYS = 2;

    private final CandidateMapper candidateMapper;
    private final CandidateDailyStatsMapper dailyStatsMapper;

    /**
     * 每 30 分钟执行一次日汇总。
     */
    @Scheduled(cron = "0 */30 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void aggregateDaily() {
        long existing = dailyStatsMapper.selectCount(null);
        int days = existing == 0 ? BACKFILL_DAYS : REFRESH_DAYS;

        LocalDate today = DateUtils.today();
        int totalRows = 0;
        for (int i = days - 1; i >= 0; i--) {
            totalRows += aggregateDay(today.minusDays(i));
        }
        log.info("候选人日汇总完成: days={}, totalRows={}", days, totalRows);
    }

    private int aggregateDay(LocalDate day) {
        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end = day.plusDays(1).atStartOfDay();

        // 幂等重建：先删当天旧数据，再按"阶段 × 来源"统计插入
        dailyStatsMapper.delete(new LambdaQueryWrapper<CandidateDailyStats>()
                .eq(CandidateDailyStats::getStatDate, day));

        List<Map<String, Object>> rows = candidateMapper.dailyStageSourceCounts(start, end);
        int inserted = 0;
        for (Map<String, Object> row : rows) {
            CandidateDailyStats stats = new CandidateDailyStats();
            stats.setStatDate(day);
            stats.setStage(toInt(row.get("stage")));
            stats.setSource(toInt(row.get("source")));
            stats.setCandidateCount((int) toLong(row.get("count")));
            dailyStatsMapper.insert(stats);
            inserted++;
        }
        return inserted;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    private long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0L; }
    }
}
