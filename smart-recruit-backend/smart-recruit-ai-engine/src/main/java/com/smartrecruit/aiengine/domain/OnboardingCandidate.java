package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 用于留任风险分析的入职候选人数据。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingCandidate {

    /**
     * 员工 ID。
     */
    private Long employeeId;
    /**
     * 员工姓名。
     */
    private String name;
    /**
     * 所属部门。
     */
    private String department;
    /**
     * 岗位名称。
     */
    private String position;
    /**
     * 入职日期。
     */
    private LocalDate onboardDate;
    /**
     * 薪资（元）。
     */
    private Double salary;
    /**
     * 学历水平。
     */
    private String educationLevel;
    /**
     * 工作年限。
     */
    private Integer yearsOfExperience;
    /**
     * 历史工作次数。
     */
    private Integer previousJobCount;
    /**
     * 平均在职时长（月）。
     */
    private Double avgTenureMonths;
    /**
     * 入职流程完成度（0-100，流程未完成风险更高）。
     */
    private Integer onboardingCompletion;
    /**
     * 通勤距离（公里）。
     */
    private String commuteDistance;
    /**
     * 是否为内推入职。
     */
    private Boolean isReferral;
}
