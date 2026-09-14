package com.smartrecruit.recruitment.controller;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.recruitment.dto.response.CandidateDailyStatVO;
import com.smartrecruit.recruitment.dto.response.ChannelStatVO;
import com.smartrecruit.recruitment.dto.response.FunnelStageVO;
import com.smartrecruit.recruitment.dto.response.MonthlyTrendVO;
import com.smartrecruit.recruitment.enums.RecruitmentEnums;
import com.smartrecruit.recruitment.entity.CandidateDailyStats;
import com.smartrecruit.recruitment.repository.CandidateDailyStatsMapper;
import com.smartrecruit.recruitment.repository.CandidateMapper;
import com.smartrecruit.common.util.DateUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * 招聘分析数据控制器（内部服务）。
 *
 * <p>基于候选人真实数据提供漏斗、渠道、趋势等聚合查询，
 * 供 Talent 模块分析服务通过 Feign 调用。</p>
 *
 * @since 2026-04-05
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class RecruitmentAnalyticsController {

    private static final int MAX_TREND_MONTHS = 24;

    private final CandidateMapper candidateMapper;
    private final CandidateDailyStatsMapper candidateDailyStatsMapper;

    /**
     * 招聘漏斗：指定时间范围内新增候选人的阶段分布。
     */
    @GetMapping("/funnel")
    public ApiResponse<List<FunnelStageVO>> funnel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        TimeRange range = TimeRange.parse(startDate, endDate);
        Map<Integer, Long> counts = new HashMap<>();
        for (Map<String, Object> row : candidateMapper.stageDistributionRange(range.start(), range.end())) {
            Integer stage = toInt(row.get("stage"));
            Long count = toLong(row.get("count"));
            if (stage != null) {
                counts.merge(stage, count, Long::sum);
            }
        }

        List<FunnelStageVO> funnel = new ArrayList<>();
        for (RecruitmentEnums.CandidateStatus status : RecruitmentEnums.CandidateStatus.values()) {
            funnel.add(FunnelStageVO.builder()
                    .name(status.getLabel())
                    .stage(status.getCode())
                    .count(counts.getOrDefault(status.getCode(), 0L))
                    .build());
        }
        return ApiResponse.success(funnel);
    }

    /**
     * 渠道分析：指定时间范围内新增候选人的来源分布与入职转化。
     */
    @GetMapping("/channels")
    public ApiResponse<List<ChannelStatVO>> channels(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        TimeRange range = TimeRange.parse(startDate, endDate);
        Map<Integer, Long> candidateCounts = groupByInt(
                candidateMapper.sourceDistributionRange(range.start(), range.end()), "source");
        Map<Integer, Long> hireCounts = groupByInt(
                candidateMapper.sourceHireDistributionRange(range.start(), range.end()), "source");

        List<ChannelStatVO> channels = new ArrayList<>();
        for (RecruitmentEnums.CandidateSource source : RecruitmentEnums.CandidateSource.values()) {
            long candidates = candidateCounts.getOrDefault(source.getCode(), 0L);
            long hires = hireCounts.getOrDefault(source.getCode(), 0L);
            double hireRate = candidates > 0 ? Math.round(hires * 10000.0 / candidates) / 100.0 : 0.0;
            channels.add(ChannelStatVO.builder()
                    .source(source.getCode())
                    .sourceName(source.getLabel())
                    .candidateCount(candidates)
                    .hireCount(hires)
                    .hireRate(hireRate)
                    .build());
        }
        return ApiResponse.success(channels);
    }

    /**
     * 月度新增候选人趋势（最近 N 个月，缺失月份补 0）。
     */
    @GetMapping("/candidate-trend")
    public ApiResponse<List<MonthlyTrendVO>> candidateTrend(
            @RequestParam(value = "months", defaultValue = "12") int months) {
        int actualMonths = Math.max(1, Math.min(months, MAX_TREND_MONTHS));
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : candidateMapper.monthlyTrendRange(actualMonths)) {
            String month = String.valueOf(row.get("month"));
            counts.put(month, toLong(row.get("count")));
        }

        List<MonthlyTrendVO> trend = new ArrayList<>();
        YearMonth current = DateUtils.currentYearMonth();
        for (int i = actualMonths - 1; i >= 0; i--) {
            String month = current.minusMonths(i).toString();
            trend.add(MonthlyTrendVO.builder()
                    .month(month)
                    .candidateCount(counts.getOrDefault(month, 0L))
                    .build());
        }
        return ApiResponse.success(trend);
    }

    /**
     * 候选人日汇总查询（内部服务）：按日期返回阶段 × 来源的聚合行。
     *
     * <p>分析页只读本接口，数据由定时任务预聚合。</p>
     */
    @GetMapping("/candidate-daily")
    public ApiResponse<List<CandidateDailyStatVO>> candidateDaily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        TimeRange range = TimeRange.parse(startDate, endDate);
        LambdaQueryWrapper<CandidateDailyStats> wrapper = new LambdaQueryWrapper<>();
        if (range.start() != null) {
            wrapper.ge(CandidateDailyStats::getStatDate, range.start().toLocalDate());
        }
        if (range.end() != null) {
            wrapper.lt(CandidateDailyStats::getStatDate, range.end().toLocalDate());
        }
        wrapper.orderByAsc(CandidateDailyStats::getStatDate);

        List<CandidateDailyStatVO> result = candidateDailyStatsMapper.selectList(wrapper).stream()
                .map(s -> new CandidateDailyStatVO(
                        s.getStatDate(), s.getStage(), s.getSource(), s.getCandidateCount()))
                .toList();
        return ApiResponse.success(result);
    }

    // ==================== 工具方法 ====================

    private Map<Integer, Long> groupByInt(List<Map<String, Object>> rows, String key) {
        Map<Integer, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer k = toInt(row.get(key));
            Long v = toLong(row.get("count"));
            if (k != null) {
                result.merge(k, v, Long::sum);
            }
        }
        return result;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Long toLong(Object obj) {
        if (obj == null) return 0L;
        if (obj instanceof Number n) return n.longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return 0L; }
    }

    /**
     * 时间范围（半开区间 [start, end)）。
     */
    private record TimeRange(LocalDateTime start, LocalDateTime end) {

        static TimeRange parse(String startDate, String endDate) {
            LocalDateTime start = null;
            LocalDateTime end = null;
            LocalDate startParsed = DateUtils.parseDate(startDate);
            LocalDate endParsed = DateUtils.parseDate(endDate);
            if (startParsed != null) {
                start = startParsed.atStartOfDay();
            }
            if (endParsed != null) {
                end = endParsed.plusDays(1).atStartOfDay();
            }
            return new TimeRange(start, end);
        }
    }
}
