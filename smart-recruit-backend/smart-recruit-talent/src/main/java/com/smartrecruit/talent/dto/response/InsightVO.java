package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 招聘分析洞察（基于真实统计自动生成）。
 *
 * @since 2026-04-05
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightVO {

    /** 洞察 ID。 */
    private Integer id;

    /** 图标标识：trend/connection/money/user/data/warning。 */
    private String icon;

    /** 标题。 */
    private String title;

    /** 描述。 */
    private String description;

    /** 趋势方向：UP/DOWN/STABLE。 */
    private String trend;

    /** 关键数值。 */
    private String value;

    /** 是否由 AI（LLM）生成：true=AI 生成，false=规则模板。 */
    private Boolean aiGenerated;
}
