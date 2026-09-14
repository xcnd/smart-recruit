package com.smartrecruit.talent.task;

import com.smartrecruit.talent.service.AnalyticsDailySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据分析日汇总快照定时同步任务。
 *
 * <p>每 10 分钟从 recruitment / offer / interview 服务拉取一次日汇总，
 * 落地到 talent 本地库；启动 10 秒后先执行一次，保证页面第一时间有数据。</p>
 *
 * @since 2026-04-10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AnalyticsDailySyncTask {

    private final AnalyticsDailySyncService syncService;

    /**
     * 每 10 分钟同步一次（启动 10 秒后首次执行）。
     */
    @Scheduled(initialDelay = 10_000, fixedDelay = 600_000)
    public void syncDailyData() {
        try {
            syncService.syncDailyData();
        } catch (Exception e) {
            log.error("数据分析日汇总快照同步任务异常: error={}", e.getMessage(), e);
        }
    }
}
