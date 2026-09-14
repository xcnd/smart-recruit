package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 包含关键影响因素的Offer接受度预测。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferPredictionVO {

    /** Offer ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long offerId;

    /** 接受概率（0-100）。 */
    private Integer acceptanceProbability;

    /** 预测置信度级别：高、中、低。 */
    private Integer confidenceLevel;

    /** 风险级别：LOW、MEDIUM、HIGH（LLM 预测结果）。 */
    private String riskLevel;

    /** 影响预测的主要因素。 */
    private List<Factor> factors;

    /** 提高接受度的建议行动。 */
    private String suggestion;

    /**
     * 个体影响因素。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Factor {
        /** 影响因素名称。 */
        private String name;
        /** 影响程度：POSITIVE、NEGATIVE、NEUTRAL。 */
        private String impact;
        /** 影响权重（0-1）。 */
        private BigDecimal weight;
    }
}
