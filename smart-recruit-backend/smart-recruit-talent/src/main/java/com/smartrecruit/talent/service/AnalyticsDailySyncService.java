package com.smartrecruit.talent.service;

/**
 * 数据分析日汇总快照同步服务。
 *
 * <p>定时从 recruitment / offer / interview 三个服务拉取日汇总数据，
 * 落地到 talent 本地库，数据分析页只读本地快照。</p>
 *
 * @since 2026-04-10
 */
public interface AnalyticsDailySyncService {

    /**
     * 执行一次全量同步（近 400 天，覆盖 12 个月趋势）。
     */
    void syncDailyData();

    /**
     * 本地快照是否已有数据（用于首次部署时判断是否降级远程查询）。
     */
    boolean hasSnapshotData();
}
