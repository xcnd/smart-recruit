package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据分析页聚合视图对象。
 *
 * <p>一次接口调用返回页面所需全部指标，由各业务模块真实数据聚合而成。</p>
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewVO {

    /** 统计周期（yyyy-MM-dd ~ yyyy-MM-dd）。 */
    private String startDate;

    private String endDate;

    /** KPI 卡片。 */
    private List<AnalyticsKpiVO> kpis;

    /** 转化漏斗。 */
    private List<FunnelStageVO> funnel;

    /** 渠道分析。 */
    private List<ChannelVO> channels;

    /** 近 12 个月候选人/入职趋势。 */
    private List<TrendPointVO> candidateTrend;

    /** 近 12 个月 Offer 趋势。 */
    private List<OfferTrendPointVO> offerTrend;

    /** AI 洞察。 */
    private List<InsightVO> insights;
}
