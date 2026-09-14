package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 月度候选人趋势视图对象。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 月份（yyyy-MM）。 */
    private String month;

    /** 该月新增候选人数。 */
    private Long candidateCount;
}
