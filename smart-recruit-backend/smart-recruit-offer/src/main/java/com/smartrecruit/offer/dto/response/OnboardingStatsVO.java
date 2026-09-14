package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入职统计数据视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingStatsVO {

    /** 入职总人数。 */
    private long totalCount;

    /** 待入职人数。 */
    private long pendingCount;

    /** 入职中人数。 */
    private long activeCount;

    /** 已完成人数。 */
    private long doneCount;

    /** 有风险人数。 */
    private long atRiskCount;

    /** 低风险人数。 */
    private long lowRiskCount;

    /** 中等风险人数。 */
    private long mediumRiskCount;

    /** 高风险人数。 */
    private long highRiskCount;
}
