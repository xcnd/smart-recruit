package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 招聘周期数据，包含月度招聘周期时长和目标。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentCycleVO {

    /** 月份（yyyy-MM 格式）。 */
    private String month;

    /** 实际招聘周期（天）。 */
    private Integer cycle;

    /** 目标招聘周期（天）。 */
    private Integer target;
}
