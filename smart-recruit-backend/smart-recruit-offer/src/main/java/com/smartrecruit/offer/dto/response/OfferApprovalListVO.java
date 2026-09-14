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

/**
 * 审批工作台列表视图对象。
 *
 * <p>用于审批管理页面列表展示，包含Offer基本信息和最新审批状态。</p>
 *
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferApprovalListVO {

    /** Offer ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long offerId;

    /** Offer 编号。 */
    private String offerNo;

    /** 候选人 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long candidateId;

    /** 候选人姓名。 */
    private String candidateName;

    /** 职位名称。 */
    private String positionTitle;

    /** 部门名称。 */
    private String departmentName;

    /** 录用职级。 */
    private String level;

    /** 基本月薪（元）。 */
    private BigDecimal baseSalary;

    /** 年薪总包（元）。 */
    private BigDecimal totalPackage;

    /** 期望入职日期。 */
    private LocalDate expectedOnboardDate;

    /** Offer 状态。 */
    private Integer status;

    /** Offer 状态文本。 */
    private String statusLabel;

    /** 提交审批时间（Offer状态变为PENDING的时间）。 */
    private LocalDateTime submitTime;

    /** 最新审批记录。 */
    private ApprovalInfo latestApproval;

    /** 当前审批级别（由后端根据已有审批记录自动计算）。 */
    private Integer currentLevel;

    /** 创建人姓名（申请人，由 createBy 用户ID解析而来）。 */
    private String creatorName;
    /** 创建时间。 */
    private LocalDateTime createTime;

    /**
     * 审批记录摘要。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalInfo {
        /** 审批记录ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long id;

        /** 审批人ID。 */
        @JsonSerialize(using = ToStringSerializer.class)
        private Long approverId;

        /** 审批人姓名。 */
        private String approverName;

        /** 审批人角色，如"团队负责人"、"HR经理"。 */
        private String approverRole;

        /** 审批级别。 */
        private Integer approvalLevel;

        /** 审批状态：0=PENDING, 1=APPROVED, 2=REJECTED。 */
        private Integer status;

        /** 审批状态文本。 */
        private String statusLabel;

        /** 审批意见。 */
        private String comment;

        /** 审批时间。 */
        private LocalDateTime approveTime;
    }
}
