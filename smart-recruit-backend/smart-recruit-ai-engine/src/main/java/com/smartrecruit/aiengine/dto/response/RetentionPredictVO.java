package com.smartrecruit.aiengine.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 新员工留存风险预测视图对象。
 *
 * @param retentionScore6M  6 个月留任概率（0-100）
 * @param retentionScore12M 12 个月留任概率（0-100）
 * @param riskLevel         风险级别：LOW / MEDIUM / HIGH
 * @param riskFactors       风险因素及影响权重（0-1）
 * @param recommendation    建议干预措施
 * @param interventions     可执行的干预动作列表
 * @since 2026-04-10
 */
public record RetentionPredictVO(
        Double retentionScore6M,
        Double retentionScore12M,
        String riskLevel,
        Map<String, Double> riskFactors,
        String recommendation,
        List<String> interventions
) {
}
