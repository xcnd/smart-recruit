package com.smartrecruit.offer.dto.response;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 包含薪资明细和审批历史的Offer详情视图。
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferDetailVO {

    /** Offer ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 求职申请 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicationId;
    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;
    /** 候选人姓名。 */
    private String candidateName;
    /** 职位 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long jobPositionId;
    /** Offer 编号。 */
    private String offerNo;
    /** 职位名称。 */
    private String positionTitle;
    /** 部门名称。 */
    private String departmentName;
    /** 录用职级。 */
    private String level;
    /** 基本月薪（元）。 */
    private BigDecimal baseSalary;
    /** 年终奖月数。 */
    private Integer bonusMonths;
    /** 股票期权数量。 */
    private BigDecimal stockOptions;
    /** 签约奖金（元）。 */
    private BigDecimal signOnBonus;
    /** 年薪总包（元）。 */
    private BigDecimal totalPackage;
    /** Offer 状态。 */
    private Integer status;
    /** 期望入职日期。 */
    private LocalDate expectedOnboardDate;
    /** 有效期截止日。 */
    private LocalDate validUntil;
    /** Offer 发送时间。 */
    private LocalDateTime sendTime;
    /** 候选人回复时间。 */
    private LocalDateTime respondTime;
    /** 拒绝原因。 */
    private String declineReason;
    /** 薪资结构明细（JSON）。 */
    private Object salaryStructure;
    /** 审批历史列表。 */
    private List<OfferApprovalVO> approvals;
    /** 创建人 ID。 */
    private String createBy;
    /** 创建人姓名（由 createBy 用户ID解析而来）。 */
    private String creatorName;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferApprovalVO {
        /** 审批记录 ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;
        /** 审批人 ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long approverId;
        /** 审批人姓名。 */
        private String approverName;
        /** 审批人角色。 */
        private String approverRole;
        /** 审批级别。 */
        private Integer approvalLevel;
        /** 审批状态：PENDING、APPROVED、REJECTED。 */
        private Integer status;
        /** 审批意见。 */
        private String comment;
        /** 审批时间。 */
        private LocalDateTime approveTime;
    }
}
