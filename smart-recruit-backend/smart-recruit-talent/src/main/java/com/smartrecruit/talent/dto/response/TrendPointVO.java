package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 月度招聘趋势数据点。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointVO {

    /** 月份（yyyy-MM）。 */
    private String month;

    /** 新增候选人数。 */
    private Long candidateCount;

    /** 完成入职人数。 */
    private Long onboardCount;
}
