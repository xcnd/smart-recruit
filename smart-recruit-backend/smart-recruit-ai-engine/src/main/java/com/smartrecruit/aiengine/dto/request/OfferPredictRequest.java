package com.smartrecruit.aiengine.dto.request;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.OfferDetail;

/**
 * Offer 接受度预测请求。
 *
 * @param candidate 候选人画像
 * @param offer     Offer 详情
 * @since 2026-04-10
 */
public record OfferPredictRequest(CandidateProfile candidate, OfferDetail offer) {
}
