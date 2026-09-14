package com.smartrecruit.interview.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.interview.dto.response.InterviewDailyStatVO;
import com.smartrecruit.interview.entity.InterviewDailyStats;
import com.smartrecruit.interview.repository.InterviewDailyStatsMapper;
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
 * 面试分析数据控制器（内部服务）。
 *
 * <p>为 Talent 分析页提供面试日汇总查询。数据由定时任务按天预聚合到
 * {@code interview_daily_stats} 表，本接口只做只读查询、不参与实时计算，
 * 避免高频统计对业务库造成压力。</p>
 *
 * <p>时间参数统一遵循闭区间约定 {@code [startDate, endDate]}：
 * 起始与结束日期当天均包含在内，内部转换为 {@code [start, end + 1 天)}
 * 执行查询，与预聚合任务及其他分析接口的统计口径保持一致。
 * 日期解析统一委托 {@link DateUtils}，保证与服务内其他模块的格式及规则一致。</p>
 *
 * @since 2026-04-06
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
public class InterviewAnalyticsController {

    private final InterviewDailyStatsMapper interviewDailyStatsMapper;

    /**
     * 查询面试日汇总数据。
     *
     * @param startDate 起始日期（含），格式 yyyy-MM-dd，可空
     * @param endDate   结束日期（含），格式 yyyy-MM-dd，可空
     * @return 按统计日期升序排列的面试日汇总列表
     */
    @GetMapping("/interview-daily")
    public ApiResponse<List<InterviewDailyStatVO>> interviewDaily(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDate start = DateUtils.parseDate(startDate);
        LocalDate endExclusive = DateUtils.parseDate(endDate);

        LambdaQueryWrapper<InterviewDailyStats> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(InterviewDailyStats::getStatDate, start);
        }
        if (endExclusive != null) {
            // 结束日期含当天：转换为 (endDate, endDate + 1 天) 的上界
            wrapper.lt(InterviewDailyStats::getStatDate, endExclusive.plusDays(1));
        }
        wrapper.orderByAsc(InterviewDailyStats::getStatDate);

        List<InterviewDailyStatVO> result = interviewDailyStatsMapper.selectList(wrapper).stream()
                .map(s -> new InterviewDailyStatVO(
                        s.getStatDate(), s.getTotalCount(), s.getPassedCount(), s.getCancelledCount()))
                .toList();
        return ApiResponse.success(result);
    }
}
