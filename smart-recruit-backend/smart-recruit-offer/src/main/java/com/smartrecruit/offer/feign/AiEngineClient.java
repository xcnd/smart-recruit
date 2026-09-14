package com.smartrecruit.offer.feign;

import com.smartrecruit.aiengine.dto.request.OfferPredictRequest;
import com.smartrecruit.aiengine.dto.request.RetentionPredictRequest;
import com.smartrecruit.aiengine.dto.response.PredictOfferVO;
import com.smartrecruit.aiengine.dto.response.RetentionPredictVO;
import com.smartrecruit.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * AI 引擎能力客户端（Offer 接受度预测 / 新员工留存预测）。
 *
 * @since 2026-04-10
 */
@HttpExchange("/api/v1/agent-capabilities")
public interface AiEngineClient {

    /**
     * AI 预测候选人接受 Offer 的概率与建议（LLM 优先）。
     */
    @PostExchange("/offer/predict")
    ApiResponse<PredictOfferVO> predictOffer(@RequestBody OfferPredictRequest request);

    /**
     * AI 预测新员工留存风险与留任概率（LLM 优先）。
     */
    @PostExchange("/retention/predict")
    ApiResponse<RetentionPredictVO> predictRetention(@RequestBody RetentionPredictRequest request);
}
