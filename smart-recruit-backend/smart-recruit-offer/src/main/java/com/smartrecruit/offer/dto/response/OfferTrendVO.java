package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度 Offer 趋势视图对象。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferTrendVO {

    /** 月份（yyyy-MM）。 */
    private String month;

    /** 当月发送 Offer 数。 */
    private Long sentCount;

    /** 当月发送的 Offer 中被接受数。 */
    private Long acceptedCount;

    /** 当月发送的 Offer 平均确认周期（天）。 */
    private Double avgConfirmDays;
}
