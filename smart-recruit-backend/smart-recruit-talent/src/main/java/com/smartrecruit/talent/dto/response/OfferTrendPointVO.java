package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度 Offer 趋势数据点。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferTrendPointVO {

    /** 月份（yyyy-MM）。 */
    private String month;

    /** 发送 Offer 数。 */
    private Long sentCount;

    /** 接受数。 */
    private Long acceptedCount;

    /** 平均确认周期（天）。 */
    private Double avgConfirmDays;
}
