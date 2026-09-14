package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.CandidateProfile;
import com.smartrecruit.aiengine.domain.OfferDetail;
import com.smartrecruit.aiengine.domain.PredictionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Offer预测智能体——模拟AI驱动的Offer接受度预测。
 *
 * <p>根据Offer详情分析候选人画像，预测Offer接受可能性，
 * 识别影响因素并提出改进建议。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
public class OfferPredictorAgent {

    /**
     * 基于候选人和Offer详情预测Offer接受概率。
     *
     * @param candidate   候选人画像
     * @param offerDetail 正在发出的Offer
     * @return 包含接受概率和建议的预测结果
     */
    public PredictionResult predict(CandidateProfile candidate, OfferDetail offerDetail) {
        log.info("OfferPredictorAgent: predicting acceptance for candidate={}, offer={}",
                candidate.getName(), offerDetail.getOfferId());
        simulateLatency(100, 450);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        double baseProb = rnd.nextDouble(55.0, 90.0);

        Map<String, Double> factorInfluences = new LinkedHashMap<>();
        factorInfluences.put("compensationCompetitiveness", Math.round(rnd.nextDouble(-15.0, 15.0) * 10.0) / 10.0);
        factorInfluences.put("roleAlignment", Math.round(rnd.nextDouble(-5.0, 10.0) * 10.0) / 10.0);
        factorInfluences.put("locationPreference", Math.round(rnd.nextDouble(-10.0, 8.0) * 10.0) / 10.0);
        factorInfluences.put("companyBrand", Math.round(rnd.nextDouble(0.0, 12.0) * 10.0) / 10.0);
        factorInfluences.put("growthOpportunity", Math.round(rnd.nextDouble(-3.0, 10.0) * 10.0) / 10.0);

        double adjustedProb = baseProb + factorInfluences.values().stream()
                .mapToDouble(Double::doubleValue).sum();
        adjustedProb = Math.max(5.0, Math.min(98.0, adjustedProb));
        adjustedProb = Math.round(adjustedProb * 10.0) / 10.0;

        String riskLevel;
        if (adjustedProb >= 80.0) {
            riskLevel = "LOW";
        } else if (adjustedProb >= 60.0) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }

        Map<String, Object> suggestions = new LinkedHashMap<>();
        if (factorInfluences.get("compensationCompetitiveness") < 0) {
            suggestions.put("compensation", "Consider increasing total package by 5-10%");
        }
        if (factorInfluences.get("locationPreference") < 0) {
            suggestions.put("location", "Offer remote/hybrid work option if possible");
        }
        suggestions.put("followUp", "Schedule a personal call within 48 hours of offer delivery");

        PredictionResult result = PredictionResult.builder()
                .candidateId(offerDetail.getCandidateId())
                .offerId(offerDetail.getOfferId())
                .acceptanceProbability(adjustedProb)
                .factorInfluences(factorInfluences)
                .riskLevel(riskLevel)
                .recommendation(adjustedProb >= 70.0
                        ? "Proceed with offer delivery"
                        : "Consider adjusting offer terms before delivery")
                .suggestions(suggestions)
                .build();

        log.info("OfferPredictorAgent: prediction complete, probability={}%, riskLevel={}",
                adjustedProb, riskLevel);
        return result;
    }

    private void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextLong(minMs, maxMs));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
