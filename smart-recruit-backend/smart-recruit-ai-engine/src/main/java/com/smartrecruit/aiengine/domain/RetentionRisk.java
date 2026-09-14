package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 留任风险评估结果。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionRisk {

    /**
     * 员工 ID。
     */
    private Long employeeId;
    /**
     * 留任风险评分（0-100，越高越危险）。
     */
    private Double riskScore;
    /**
     * 风险级别：LOW、MEDIUM、HIGH。
     */
    private String riskLevel;
    /**
     * 风险因素列表。
     */
    private Map<String, Double> riskFactors;
    /**
     * 主要风险因素。
     */
    private String primaryRiskFactor;
    /**
     * 建议干预措施。
     */
    private String recommendation;
    /**
     * 预计留任月数。
     */
    private Integer predictedTenureMonths;
}
