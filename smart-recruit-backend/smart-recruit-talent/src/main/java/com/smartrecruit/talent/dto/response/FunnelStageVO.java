package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 招聘漏斗阶段，包含数量和转化率。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunnelStageVO {

    /** 阶段码。 */
    private Integer stage;

    /** 阶段名称（中文）。 */
    private String label;

    /** 该阶段候选人数量。 */
    private Integer count;

    /** 从该阶段到下一阶段的通过率（百分比）。最终阶段为0。 */
    private Double conversionRate;

    /** 在漏斗中的排序（从1开始）。 */
    private Integer sortOrder;
}
