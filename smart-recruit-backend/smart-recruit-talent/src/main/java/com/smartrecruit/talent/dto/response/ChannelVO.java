package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 招聘渠道分析视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelVO {

    /** 渠道名称。 */
    private String channel;

    /** 该渠道的候选人总数。 */
    private Integer candidateCount;

    /** 占总候选人数百分比。 */
    private Double percentage;

    /** 该渠道成功录用的数量。 */
    private Integer hireCount;

    /** 转化率（录用数 / 候选人数 * 100）。 */
    private Double conversionRate;

    /** 人均招聘成本（人民币）。 */
    private Double costPerHire;

    /** 渠道总成本（候选人数 × 人均成本，估算）。 */
    private Double totalCost;

    /** 投入产出比（入职人数 × 岗位年化价值 ÷ 总成本）。 */
    private Double roi;
}
