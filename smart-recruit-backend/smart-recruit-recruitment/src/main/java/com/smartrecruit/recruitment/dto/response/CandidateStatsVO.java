package com.smartrecruit.recruitment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 候选人统计看板的视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 候选人总数。 */
    private int totalCount;
    /** 新候选人数量。 */
    private int newCount;
    /** 筛选中数量。 */
    private int screeningCount;
    /** 筛选通过数量。 */
    private int screenPassedCount;
    /** 面试中数量。 */
    private int interviewingCount;
    /** 已发Offer数量。 */
    private int offeredCount;
    /** 已入职数量。 */
    private int hiredCount;
    /** 已淘汰数量。 */
    private int rejectedCount;
    /** 已撤回数量。 */
    private int withdrawnCount;

    /** 来源渠道分布（渠道代码 → 数量）。 */
    @Builder.Default
    private Map<String, Long> sourceDistribution = new LinkedHashMap<>();

    /** 月度趋势（最近12个月）。 */
    @Builder.Default
    private List<MonthlyTrend> monthlyTrend = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String month;
        private Long count;
    }
}
