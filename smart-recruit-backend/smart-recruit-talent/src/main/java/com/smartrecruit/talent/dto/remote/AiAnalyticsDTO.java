package com.smartrecruit.talent.dto.remote;

import java.util.List;

/**
 * AI 数据分析洞察请求/响应 DTO（对应 ai-engine 的 analytics/insights 能力）。
 *
 * @since 2026-04-09
 */
public final class AiAnalyticsDTO {

    private AiAnalyticsDTO() {
    }

    public record Kpi(String key, String label, Double value, Double prevValue, String unit) {
    }

    public record Funnel(String stage, String name, Long count) {
    }

    public record Channel(Integer source, String sourceName, Long candidateCount,
                          Long hireCount, Double hireRate) {
    }

    public record Trend(String date, Long count) {
    }

    public record Request(String startDate, String endDate,
                          List<Kpi> kpis, List<Funnel> funnel, List<Channel> channels,
                          List<Trend> candidateTrend, List<Trend> onboardingTrend) {
    }

    public record Result(String title, String description, String trend, String value) {
    }
}
