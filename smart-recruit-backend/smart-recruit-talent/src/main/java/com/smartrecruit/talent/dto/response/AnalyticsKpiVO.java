package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析页 KPI 卡片数据。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsKpiVO {

    /** KPI 标识。 */
    private String key;

    /** 指标名称。 */
    private String label;

    /** 当前周期数值。 */
    private Double value;

    /** 上一周期数值。 */
    private Double prevValue;

    /** 环比变化百分比（上一周期为 0 时为 null）。 */
    private Double changePercent;

    /** 展示单位（如 天、%）。 */
    private String unit;
}
