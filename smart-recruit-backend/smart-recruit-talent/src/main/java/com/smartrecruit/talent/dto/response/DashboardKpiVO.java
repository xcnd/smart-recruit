package com.smartrecruit.talent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 仪表盘KPI汇总及趋势。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardKpiVO {

    /** 开放招聘的职位总数。 */
    private Integer jobCount;
    /** 职位数趋势：如 "+5"。 */
    private String jobCountTrend;

    /** 今日新增候选人。 */
    private Integer todayCandidateCount;

    /** 昨日新增候选人。 */
    private Integer yesterdayCandidateCount;

    /** 收到的申请总数（本月）。 */
    private Integer applicationCount;
    /** 申请数趋势。 */
    private String applicationCountTrend;

    /** 待面试数量。 */
    private Integer pendingInterview;
    /** 待面试趋势。 */
    private String pendingInterviewTrend;

    /** 待发Offer数量。 */
    private Integer pendingOffer;
    /** 待发Offer趋势。 */
    private String pendingOfferTrend;

    /** 平均到岗天数。 */
    private Double avgDaysToHire;
    /** 平均到岗天数趋势。 */
    private String avgDaysToHireTrend;

    /** 本月入职人数。 */
    private Integer hiresThisMonth;
    /** 入职人数趋势。 */
    private String hiresThisMonthTrend;
}
