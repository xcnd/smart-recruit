package com.smartrecruit.talent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.common.util.DateUtils;
import com.smartrecruit.talent.entity.AnalyticsCandidateDaily;
import com.smartrecruit.talent.entity.AnalyticsInterviewDaily;
import com.smartrecruit.talent.entity.AnalyticsOfferDaily;
import com.smartrecruit.talent.feign.InterviewClient;
import com.smartrecruit.talent.feign.OfferClient;
import com.smartrecruit.talent.feign.RecruitmentClient;
import com.smartrecruit.talent.repository.AnalyticsCandidateDailyMapper;
import com.smartrecruit.talent.repository.AnalyticsInterviewDailyMapper;
import com.smartrecruit.talent.repository.AnalyticsOfferDailyMapper;
import com.smartrecruit.talent.service.AnalyticsDailySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据分析日汇总快照同步服务实现。
 *
 * <p>每次同步拉取近 400 天的日汇总（覆盖 12 个月趋势 + 当前周期），
 * 采用「先删窗口再批量插入」的幂等策略；单个数据源失败不影响其它数据源。</p>
 *
 * @since 2026-04-10
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsDailySyncServiceImpl implements AnalyticsDailySyncService {

    /** 同步窗口：近 400 天（覆盖 12 个月趋势）。 */
    private static final int SYNC_WINDOW_DAYS = 400;

    private final RecruitmentClient recruitmentClient;
    private final OfferClient offerClient;
    private final InterviewClient interviewClient;
    private final AnalyticsCandidateDailyMapper candidateDailyMapper;
    private final AnalyticsOfferDailyMapper offerDailyMapper;
    private final AnalyticsInterviewDailyMapper interviewDailyMapper;

    /** 执行一次全量同步。 */
    @Override
    public void syncDailyData() {
        LocalDate start = DateUtils.today().minusDays(SYNC_WINDOW_DAYS);
        LocalDate endExclusive = DateUtils.today().plusDays(1);
        log.info("开始同步数据分析日汇总快照: start={}, endExclusive={}",
                start, endExclusive);
        syncCandidateDaily(start, endExclusive);
        syncOfferDaily(start, endExclusive);
        syncInterviewDaily(start, endExclusive);
        log.info("数据分析日汇总快照同步完成");
    }

    /** 本地快照是否已有数据。 */
    @Override
    public boolean hasSnapshotData() {
        try {
            return candidateDailyMapper.selectCount(null) > 0
                    || offerDailyMapper.selectCount(null) > 0
                    || interviewDailyMapper.selectCount(null) > 0;
        } catch (Exception e) {
            log.warn("检查日汇总快照数据失败: {}", e.getMessage());
            return false;
        }
    }

    private void syncCandidateDaily(LocalDate start, LocalDate endExclusive) {
        try {
            ApiResponse<List<Map<String, Object>>> resp = recruitmentClient.getCandidateDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1)));
            List<Map<String, Object>> rows = resp != null ? resp.data() : null;
            if (rows == null) {
                log.warn("候选人日汇总接口返回空，跳过");
                return;
            }
            List<AnalyticsCandidateDaily> entities = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                AnalyticsCandidateDaily e = new AnalyticsCandidateDaily();
                e.setStatDate(parseDate(row.get("statDate")));
                e.setStage(toInt(row.get("stage")));
                e.setSource(toInt(row.get("source")));
                e.setCandidateCount(toInt(row.get("candidateCount")));
                if (e.getStatDate() != null) {
                    entities.add(e);
                }
            }
            replaceWindow(candidateDailyMapper, "候选人日汇总",
                    start, endExclusive, entities);
        } catch (Exception e) {
            log.error("同步候选人日汇总失败: error={}", e.getMessage(), e);
        }
    }

    private void syncOfferDaily(LocalDate start, LocalDate endExclusive) {
        try {
            ApiResponse<List<Map<String, Object>>> resp = offerClient.getOfferDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1)));
            List<Map<String, Object>> rows = resp != null ? resp.data() : null;
            if (rows == null) {
                log.warn("Offer 日汇总接口返回空，跳过");
                return;
            }
            List<AnalyticsOfferDaily> entities = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                AnalyticsOfferDaily e = new AnalyticsOfferDaily();
                e.setStatDate(parseDate(row.get("statDate")));
                e.setSentCount(toInt(row.get("sentCount")));
                e.setAcceptedCount(toInt(row.get("acceptedCount")));
                e.setDeclinedCount(toInt(row.get("declinedCount")));
                e.setPendingCount(toInt(row.get("pendingCount")));
                e.setOnboardCount(toInt(row.get("onboardCount")));
                e.setConfirmTotalDays(toLong(row.get("confirmTotalDays")));
                e.setConfirmCount(toInt(row.get("confirmCount")));
                if (e.getStatDate() != null) {
                    entities.add(e);
                }
            }
            replaceWindow(offerDailyMapper, "Offer 日汇总",
                    start, endExclusive, entities);
        } catch (Exception e) {
            log.error("同步 Offer 日汇总失败: error={}", e.getMessage(), e);
        }
    }

    private void syncInterviewDaily(LocalDate start, LocalDate endExclusive) {
        try {
            ApiResponse<List<Map<String, Object>>> resp = interviewClient.getInterviewDaily(
                    DateUtils.formatDate(start), DateUtils.formatDate(endExclusive.minusDays(1)));
            List<Map<String, Object>> rows = resp != null ? resp.data() : null;
            if (rows == null) {
                log.warn("面试日汇总接口返回空，跳过");
                return;
            }
            List<AnalyticsInterviewDaily> entities = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                AnalyticsInterviewDaily e = new AnalyticsInterviewDaily();
                e.setStatDate(parseDate(row.get("statDate")));
                e.setTotalCount(toInt(row.get("totalCount")));
                e.setPassedCount(toInt(row.get("passedCount")));
                e.setCancelledCount(toInt(row.get("cancelledCount")));
                if (e.getStatDate() != null) {
                    entities.add(e);
                }
            }
            replaceWindow(interviewDailyMapper, "面试日汇总",
                    start, endExclusive, entities);
        } catch (Exception e) {
            log.error("同步面试日汇总失败: error={}", e.getMessage(), e);
        }
    }

    /** 先删除窗口内旧数据，再批量插入（幂等）。 */
    private <T> void replaceWindow(com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
                                   String name, LocalDate start, LocalDate endExclusive,
                                   List<T> entities) {
        mapper.delete(new QueryWrapper<T>()
                .ge("stat_date", start)
                .lt("stat_date", endExclusive));
        for (T entity : entities) {
            mapper.insert(entity);
        }
        log.info("{}同步完成: rows={}", name, entities.size());
    }

    private LocalDate parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate date) {
            return date;
        }
        return DateUtils.parseDate(String.valueOf(value));
    }

    private Integer toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
