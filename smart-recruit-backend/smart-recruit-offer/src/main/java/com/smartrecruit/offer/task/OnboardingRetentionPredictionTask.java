package com.smartrecruit.offer.task;

import com.smartrecruit.offer.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 入职 AI 留任预测定时任务。
 *
 * <p>每 10 分钟扫描入职记录：入职 5 步数据指纹（hash）未变化则跳过，
 * 有变化或未预测的记录异步调用 AI 预测并落库；查询时直接返回保存的数据。</p>
 *
 * @since 2026-04-10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OnboardingRetentionPredictionTask {

    private final OnboardingService onboardingService;

    /**
     * 启动 30 秒后首次执行，之后每 10 分钟刷新一次。
     */
    @Scheduled(initialDelay = 30_000, fixedDelay = 600_000)
    public void refreshRetentionPredictions() {
        try {
            onboardingService.refreshRetentionPredictions();
        } catch (Exception e) {
            log.error("入职留任预测定时任务异常: error={}", e.getMessage(), e);
        }
    }
}
