package com.smartrecruit.offer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度入职趋势视图对象。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingTrendVO {

    /** 月份（yyyy-MM）。 */
    private String month;

    /** 当月完成入职的人数。 */
    private Long onboardCount;
}
