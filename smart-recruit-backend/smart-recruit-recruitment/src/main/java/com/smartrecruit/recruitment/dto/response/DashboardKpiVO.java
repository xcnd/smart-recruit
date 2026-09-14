package com.smartrecruit.recruitment.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工作台 KPI 统计视图对象。
 *
 * @since 1.0.0
 */
@Data
@Builder
public class DashboardKpiVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 在招职位数 */
    private Long activeJobCount;

    /** 今日新增候选人 */
    private Long todayNewCandidateCount;

    /** 昨日新增候选人 */
    private Long yesterdayNewCandidateCount;

    /** 本月新增候选人 */
    private Long monthlyNewCandidateCount;

    /** 面试安排数（进行中面试） */
    private Long activeInterviewCount;

    /** 待审批 Offer 数 */
    private Long pendingOfferCount;

    /** 待入职数 */
    private Long pendingOnboardingCount;

    /** 本月入职数 */
    private Long monthlyHiredCount;
}
