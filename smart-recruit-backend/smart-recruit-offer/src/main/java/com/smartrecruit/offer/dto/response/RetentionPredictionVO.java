package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 入职跟踪的员工留任预测VO。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetentionPredictionVO {

    /** 入职ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long onboardingId;

    /** 预测状态：PENDING=评估中，COMPLETED=已完成。 */
    private String status;

    /** 6个月留任概率（0-100）。 */
    private Integer retentionScore6M;

    /** 12个月留任概率（0-100）。 */
    private Integer retentionScore12M;

    /** 风险级别：低、中、高。 */
    private Integer riskLevel;

    /** 关键风险因素。 */
    private List<String> riskFactors;

    /** 建议的干预措施。 */
    private List<String> interventions;
}
