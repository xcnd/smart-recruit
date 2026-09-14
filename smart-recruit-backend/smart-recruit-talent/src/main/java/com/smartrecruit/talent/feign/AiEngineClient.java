package com.smartrecruit.talent.feign;

import com.smartrecruit.common.dto.ApiResponse;
import com.smartrecruit.talent.dto.remote.AiTalentRecommendDTO.Request;
import com.smartrecruit.talent.dto.remote.AiTalentRecommendDTO.Result;
import com.smartrecruit.talent.dto.remote.AiAnalyticsDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * AI 引擎能力客户端（人才推荐）。
 *
 * @since 2026-04-09
 */
@HttpExchange("/api/v1/agent-capabilities")
public interface AiEngineClient {

    /**
     * AI 人才推荐：为一批候选人匹配目标职位并排序。
     */
    @PostExchange("/talent/recommend")
    ApiResponse<List<Result>> recommendTalent(@RequestBody Request request);

    /**
     * AI 数据分析洞察：基于统计数据生成自然语言洞察与建议。
     */
    @PostExchange("/analytics/insights")
    ApiResponse<List<AiAnalyticsDTO.Result>> analyticsInsights(
            @RequestBody AiAnalyticsDTO.Request request);
}
