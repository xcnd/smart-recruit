package com.smartrecruit.aiengine.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用于预测分析的Offer详情。
 *
 * @author xdh
 * @since 2026-04-26
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDetail {

    /**
     * Offer ID。
     */
    private Long offerId;
    /**
     * 候选人 ID。
     */
    private Long candidateId;
    /**
     * 职位 ID。
     */
    private Long jobId;
    /**
     * 职位名称。
     */
    private String jobTitle;
    /**
     * 所属部门。
     */
    private String department;
    /**
     * 基本月薪（元）。
     */
    private Double baseSalary;
    /**
     * 年终奖金额（元）。
     */
    private Double bonusAmount;
    /**
     * 年薪总包（元）。
     */
    private Double totalPackage;
    /**
     * 工作地点。
     */
    private String location;
    /**
     * 股权/期权信息。
     */
    private String equityInfo;
}
