package com.smartrecruit.talent.service;

import com.smartrecruit.talent.dto.response.AnalyticsOverviewVO;
import com.smartrecruit.talent.dto.response.ChannelVO;
import com.smartrecruit.talent.dto.response.FunnelStageVO;
import com.smartrecruit.talent.dto.response.InsightVO;
import com.smartrecruit.talent.dto.response.RecruitmentCycleVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 招聘分析服务接口。
 *
 * <p>聚合招聘、面试、Offer、入职各模块真实数据，
 * 生成分析页所需的 KPI、漏斗、渠道、趋势与洞察。</p>
 *
 * @since 1.0.0
 */
public interface AnalyticsService {

    /**
     * 获取分析页聚合数据（KPI + 漏斗 + 渠道 + 趋势 + 洞察）。
     *
     * @param startDate 统计起始日期（含），为 null 时默认本月 1 号
     * @param endDate   统计结束日期（含），为 null 时默认今天
     */
    AnalyticsOverviewVO getOverview(LocalDate startDate, LocalDate endDate);

    /**
     * 获取招聘漏斗，包含各阶段计数和转化率。
     */
    List<FunnelStageVO> getFunnel(LocalDate startDate, LocalDate endDate);

    /**
     * 获取招聘渠道分析，包含候选人分布和转化情况。
     */
    List<ChannelVO> getChannelAnalysis(LocalDate startDate, LocalDate endDate);

    /**
     * 获取基于真实统计生成的招聘洞察。
     */
    List<InsightVO> getInsights();

    /**
     * 获取招聘周期数据，包含月度招聘周期时长和目标对比。
     *
     * @param period 时间范围（可选，兼容历史调用）
     */
    List<RecruitmentCycleVO> getRecruitmentCycle(String period);

    /**
     * 导出分析报告（CSV）。
     */
    byte[] exportReport(LocalDate startDate, LocalDate endDate);

    // ==================== 兼容旧调用（无日期参数） ====================

    default List<FunnelStageVO> getFunnel() {
        return getFunnel(null, null);
    }

    default List<ChannelVO> getChannelAnalysis() {
        return getChannelAnalysis(null, null);
    }

    default byte[] exportReport() {
        return exportReport(null, null);
    }
}
