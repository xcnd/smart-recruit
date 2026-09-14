package com.smartrecruit.offer.task;

import com.smartrecruit.offer.service.OfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Offer AI 接受度预测定时任务。
 *
 * <p>每 15 分钟批量刷新未进入终态的 Offer 接受度预测（LLM 优先），
 * 预测结果落库，Offer 详情页直接读取，避免实时调用 LLM 超时。</p>
 *
 * @since 2026-04-10
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OfferAiPredictionTask {

    private final OfferService offerService;

    /**
     * 启动 30 秒后首次执行，之后每 15 分钟刷新一次。
     */
    @Scheduled(initialDelay = 30_000, fixedDelay = 900_000)
    public void refreshPredictions() {
        try {
            offerService.refreshAiPredictions();
        } catch (Exception e) {
            log.error("Offer AI 接受度预测定时任务异常: error={}", e.getMessage(), e);
        }
    }
}
