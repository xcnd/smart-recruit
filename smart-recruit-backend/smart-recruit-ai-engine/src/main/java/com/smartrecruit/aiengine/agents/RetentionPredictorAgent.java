package com.smartrecruit.aiengine.agents;

import com.smartrecruit.aiengine.domain.OnboardingCandidate;
import com.smartrecruit.aiengine.domain.RetentionRisk;
import com.smartrecruit.aiengine.service.RetentionThresholdProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 留任预测智能体——模拟AI驱动的留任风险分析。
 *
 * <p>分析入职候选人数据以预测留任风险，
 * 识别关键风险因素并提出留任策略。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RetentionPredictorAgent {

    private final RetentionThresholdProvider thresholdProvider;

    /**
     * 预测入职候选人的留任风险。
     *
     * @param candidate 入职候选人数据
     * @return 留任风险评估
     */
    public RetentionRisk predict(OnboardingCandidate candidate) {
        log.info("RetentionPredictorAgent: predicting retention for employee={}", candidate.getEmployeeId());
        simulateLatency(80, 300);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        Map<String, Double> riskFactors = new LinkedHashMap<>();
        riskFactors.put("jobHoppingHistory", Math.round(rnd.nextDouble(0.0, 0.4) * 100.0) / 100.0);
        riskFactors.put("compensationAlignment", Math.round(rnd.nextDouble(0.0, 0.35) * 100.0) / 100.0);
        riskFactors.put("commuteDistance", Math.round(rnd.nextDouble(0.0, 0.25) * 100.0) / 100.0);
        riskFactors.put("careerGrowth", Math.round(rnd.nextDouble(0.0, 0.3) * 100.0) / 100.0);
        riskFactors.put("culturalFit", Math.round(rnd.nextDouble(0.0, 0.2) * 100.0) / 100.0);

        // 计算加权风险分数
        double riskScore = riskFactors.get("jobHoppingHistory") * 0.30
                + riskFactors.get("compensationAlignment") * 0.25
                + riskFactors.get("commuteDistance") * 0.15
                + riskFactors.get("careerGrowth") * 0.20
                + riskFactors.get("culturalFit") * 0.10;

        // 针对内推奖励进行调整
        if (candidate.getIsReferral() != null && candidate.getIsReferral()) {
            riskScore *= 0.85; // 内推员工风险较低
        }

        riskScore = Math.min(1.0, Math.max(0.0, riskScore));
        riskScore = Math.round(riskScore * 1000.0) / 1000.0;

        double lowThreshold = thresholdProvider.low();
        double mediumThreshold = thresholdProvider.medium();
        double highThreshold = thresholdProvider.high();

        String riskLevel;
        if (riskScore < lowThreshold) {
            riskLevel = "LOW";
        } else if (riskScore < mediumThreshold) {
            riskLevel = "MEDIUM";
        } else if (riskScore < highThreshold) {
            riskLevel = "HIGH";
        } else {
            riskLevel = "CRITICAL";
        }

        // 找出主要风险因素
        String primaryRiskFactor = riskFactors.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");

        // 预测在职时长
        int predictedTenure = (int) Math.round(60.0 * (1.0 - riskScore) + rnd.nextInt(-6, 6));
        predictedTenure = Math.max(3, Math.min(72, predictedTenure));

        String recommendation = switch (riskLevel) {
            case "LOW" -> "Standard onboarding and check-in schedule";
            case "MEDIUM" -> "Assign mentor and schedule monthly 1:1 for first 6 months";
            case "HIGH" -> "Implement retention plan: mentor + career path planning + quarterly review";
            case "CRITICAL" -> "Immediate intervention: executive check-in, personalized growth plan, compensation review";
            default -> "Monitor regularly";
        };

        RetentionRisk result = RetentionRisk.builder()
                .employeeId(candidate.getEmployeeId())
                .riskScore(riskScore)
                .riskLevel(riskLevel)
                .riskFactors(riskFactors)
                .primaryRiskFactor(primaryRiskFactor)
                .recommendation(recommendation)
                .predictedTenureMonths(predictedTenure)
                .build();

        log.info("RetentionPredictorAgent: prediction complete, riskScore={}, riskLevel={}",
                riskScore, riskLevel);
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
