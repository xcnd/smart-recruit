package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Offer 统计视图对象。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferAnalyticsVO {

    /** 已发送 Offer 数。 */
    private Long sentCount;

    /** 已接受数。 */
    private Long acceptedCount;

    /** 已拒绝数。 */
    private Long declinedCount;

    /** 待候选人回复数（已发送未回复）。 */
    private Long pendingCount;

    /** 周期内完成入职的人数。 */
    private Long onboardCount;

    /** 接受率（%）。 */
    private Double acceptanceRate;

    /** 平均确认周期（天，发送→候选人回复）。 */
    private Double avgConfirmDays;
}
