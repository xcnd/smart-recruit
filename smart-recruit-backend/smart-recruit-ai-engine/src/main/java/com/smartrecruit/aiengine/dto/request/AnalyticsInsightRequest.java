package com.smartrecruit.aiengine.dto.request;

import java.util.List;

/**
 * AI 数据分析洞察请求（招聘统计数据快照）。
 *
 * @param startDate       统计开始日期
 * @param endDate         统计结束日期
 * @param kpis            KPI 指标
 * @param funnel          招聘漏斗
 * @param channels        渠道数据
 * @param candidateTrend  候选人趋势
 * @param onboardingTrend 入职趋势
 * @since 2026-04-09
 */
public record AnalyticsInsightRequest(
        String startDate,
        String endDate,
        List<KpiItem> kpis,
        List<FunnelItem> funnel,
        List<ChannelItem> channels,
        List<TrendItem> candidateTrend,
        List<TrendItem> onboardingTrend) {

    public record KpiItem(String key, String label, Double value, Double prevValue, String unit) {
    }

    public record FunnelItem(String stage, String name, Long count) {
    }

    public record ChannelItem(Integer source, String sourceName, Long candidateCount,
                              Long hireCount, Double hireRate) {
    }

    public record TrendItem(String date, Long count) {
    }
}
